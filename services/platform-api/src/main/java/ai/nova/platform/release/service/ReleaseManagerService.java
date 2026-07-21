package ai.nova.platform.release.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.release.config.ReleaseProperties;
import ai.nova.platform.release.dto.ReleaseDtos.ArtifactRef;
import ai.nova.platform.release.dto.ReleaseDtos.CreateReleaseRequest;
import ai.nova.platform.release.dto.ReleaseDtos.Release;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseEventType;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.repository.ReleaseOperationRepository;
import ai.nova.platform.release.security.ReleaseAuthorizationService;
import ai.nova.platform.release.service.ReleaseManifestService.ArtifactFingerprint;
import ai.nova.platform.release.service.ReleaseManifestService.ManifestResult;
import ai.nova.platform.release.service.ReleaseStorageService.ArtifactSpec;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Release Manager orchestrates immutable release records after Merge Agent completes.
 * It never deploys, rolls back, or mutates commits/merges/approvals.
 */
@Service
public class ReleaseManagerService {

    private final ReleaseProperties properties;
    private final ReleaseAuthorizationService authorizationService;
    private final ReleaseStorageService storageService;
    private final ReleaseManifestService manifestService;
    private final ReleaseVersionService versionService;
    private final ReleaseOperationRepository operationRepository;
    private final ProjectRepository projectRepository;
    private final AuditRecordingSupport auditRecordingSupport;

    public ReleaseManagerService(
            ReleaseProperties properties,
            ReleaseAuthorizationService authorizationService,
            ReleaseStorageService storageService,
            ReleaseManifestService manifestService,
            ReleaseVersionService versionService,
            ReleaseOperationRepository operationRepository,
            ProjectRepository projectRepository,
            AuditRecordingSupport auditRecordingSupport) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.manifestService = manifestService;
        this.versionService = versionService;
        this.operationRepository = operationRepository;
        this.projectRepository = projectRepository;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional
    public Release create(CreateReleaseRequest request, AuthenticatedUser user) {
        authorizationService.require(user, ReleaseAuthorizationService.RELEASE_RUN);
        requireEnabled();
        requireProject(request.projectId(), user.getOrganizationId());

        List<UUID> mergeIds = normalizeIds(request.mergeOperationIds());
        List<UUID> approvalIds = normalizeIds(request.approvalDecisionIds());
        List<UUID> prIds = normalizeIds(request.pullRequestIds());
        List<UUID> patchIds = normalizeIds(request.patchIds());
        List<String> commits = normalizeShas(request.commitShas());
        List<ArtifactFingerprint> artifactFingerprints = toFingerprints(request.artifacts());

        String fingerprint = manifestService.contentFingerprint(
                mergeIds, approvalIds, prIds, patchIds, commits, artifactFingerprints);

        var existing = operationRepository.findByOrganizationIdAndProjectIdAndContentFingerprint(
                user.getOrganizationId(), request.projectId(), fingerprint);
        if (existing.isPresent()) {
            storageService.appendEvent(
                    existing.get().getId(),
                    ReleaseEventType.IDEMPOTENT_RETURN,
                    "Identical release content already exists",
                    Instant.now());
            return storageService.toDto(existing.get());
        }

        VersionBump bump = request.bumpType() != null ? request.bumpType() : VersionBump.PATCH;
        VersionStrategy strategy = properties.getDefaultVersionStrategy();
        ResolvedVersion version = versionService.resolve(
                user.getOrganizationId(), request.projectId(), bump, request.semanticVersion());

        if (operationRepository
                .findByOrganizationIdAndProjectIdAndSemanticVersion(
                        user.getOrganizationId(), request.projectId(), version.semanticVersion())
                .isPresent()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "RELEASE_VERSION_CONFLICT",
                    "Semantic version already used: " + version.semanticVersion());
        }

        List<ContentSpec> contents = new ArrayList<>();
        mergeIds.forEach(id -> contents.add(new ContentSpec(ReleaseContentType.MERGE_OPERATION, id, null)));
        approvalIds.forEach(id -> contents.add(new ContentSpec(ReleaseContentType.APPROVAL_DECISION, id, null)));
        prIds.forEach(id -> contents.add(new ContentSpec(ReleaseContentType.PULL_REQUEST, id, null)));
        patchIds.forEach(id -> contents.add(new ContentSpec(ReleaseContentType.PATCH, id, null)));
        commits.forEach(sha -> contents.add(new ContentSpec(ReleaseContentType.COMMIT, null, sha)));

        List<ArtifactSpec> artifacts = new ArrayList<>();
        if (request.artifacts() != null) {
            for (ArtifactRef ref : request.artifacts()) {
                if (ref == null) {
                    continue;
                }
                artifacts.add(new ArtifactSpec(ref.artifactType(), ref.artifactUri(), ref.artifactHash(), ref.label()));
            }
        }

        ReleaseOperationEntity created = storageService.createDraft(
                user.getOrganizationId(),
                request.projectId(),
                request.releaseName().trim(),
                request.description(),
                strategy,
                bump,
                version,
                fingerprint,
                user.getUserId(),
                contents,
                artifacts);
        publishAudit(user, created, AuditAction.CREATE, AuditResult.SUCCESS, Map.of("status", created.getStatus().name()));
        return storageService.toDto(created);
    }

    @Transactional
    public Release prepare(UUID releaseId, AuthenticatedUser user) {
        authorizationService.require(user, ReleaseAuthorizationService.RELEASE_RUN);
        requireEnabled();
        ReleaseOperationEntity entity = storageService.requireForOrg(releaseId, user.getOrganizationId());

        if (entity.getStatus() == ReleaseStatus.READY || entity.getStatus() == ReleaseStatus.PUBLISHED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    entity.getStatus() == ReleaseStatus.PUBLISHED
                            ? "RELEASE_ALREADY_PUBLISHED"
                            : "RELEASE_ALREADY_READY",
                    "Release is already " + entity.getStatus());
        }
        if (entity.getStatus() != ReleaseStatus.DRAFT && entity.getStatus() != ReleaseStatus.FAILED) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "RELEASE_INVALID_STATUS",
                    "Release must be DRAFT or FAILED to prepare; was " + entity.getStatus());
        }
        if (!properties.isAllowEditAfterReady()
                && entity.getManifestHash() != null
                && entity.getStatus() == ReleaseStatus.READY) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "RELEASE_ALREADY_READY", "Manifest is immutable after READY");
        }

        storageService.markPreparing(releaseId);
        publishAudit(user, entity, AuditAction.PREPARE, AuditResult.SUCCESS, Map.of("releaseId", releaseId.toString()));
        try {
            ManifestResult manifest = manifestService.build(
                    storageService.require(releaseId),
                    storageService.contents(releaseId),
                    storageService.artifacts(releaseId));
            if (entity.getManifestHash() != null
                    && !Objects.equals(entity.getManifestHash(), manifest.manifestHash())
                    && !properties.isAllowEditAfterReady()) {
                throw new ApiException(
                        HttpStatus.CONFLICT,
                        "RELEASE_MANIFEST_CHANGED",
                        "Release manifest hash changed unexpectedly");
            }
            ReleaseOperationEntity ready = storageService.markReady(releaseId, manifest);
            publishAudit(user, ready, AuditAction.READY, AuditResult.SUCCESS, Map.of("status", ready.getStatus().name()));
            return storageService.toDto(ready);
        } catch (ApiException ex) {
            storageService.markFailed(releaseId, ex.getCode(), ex.getMessage());
            publishAudit(user, entity, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", ex.getCode()));
            throw ex;
        } catch (RuntimeException ex) {
            storageService.markFailed(releaseId, "RELEASE_MANIFEST_CHANGED", ex.getMessage());
            publishAudit(
                    user, entity, AuditAction.FAIL, AuditResult.FAILURE, Map.of("errorCode", "RELEASE_MANIFEST_CHANGED"));
            throw new ApiException(HttpStatus.INTERNAL_SERVER_ERROR, "RELEASE_MANIFEST_CHANGED", ex.getMessage());
        }
    }

    @Transactional
    public Release publish(UUID releaseId, AuthenticatedUser user) {
        authorizationService.require(user, ReleaseAuthorizationService.RELEASE_RUN);
        requireEnabled();
        ReleaseOperationEntity entity = storageService.requireForOrg(releaseId, user.getOrganizationId());

        if (entity.getStatus() == ReleaseStatus.PUBLISHED) {
            throw new ApiException(HttpStatus.CONFLICT, "RELEASE_ALREADY_PUBLISHED", "Release is already published");
        }
        if (entity.getStatus() != ReleaseStatus.READY) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "RELEASE_INVALID_STATUS",
                    "Release must be READY to publish; was " + entity.getStatus());
        }
        if (entity.getManifestHash() == null || entity.getManifestJson() == null) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "RELEASE_MANIFEST_CHANGED", "Release has no frozen manifest");
        }

        // Recompute to ensure frozen manifest still matches content (immutability check).
        ManifestResult recomputed = manifestService.build(
                entity, storageService.contents(releaseId), storageService.artifacts(releaseId));
        if (!Objects.equals(entity.getManifestHash(), recomputed.manifestHash())) {
            storageService.markFailed(releaseId, "RELEASE_MANIFEST_CHANGED", "Manifest hash mismatch on publish");
            publishAudit(
                    user,
                    entity,
                    AuditAction.FAIL,
                    AuditResult.FAILURE,
                    Map.of("errorCode", "RELEASE_MANIFEST_CHANGED"));
            throw new ApiException(
                    HttpStatus.CONFLICT, "RELEASE_MANIFEST_CHANGED", "Manifest hash mismatch on publish");
        }

        ReleaseOperationEntity published = storageService.markPublished(releaseId);
        publishAudit(user, published, AuditAction.PUBLISH, AuditResult.SUCCESS, Map.of("status", published.getStatus().name()));
        return storageService.toDto(published);
    }

    @Transactional(readOnly = true)
    public List<Release> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, ReleaseAuthorizationService.RELEASE_READ);
        requireEnabled();
        List<ReleaseOperationEntity> entities;
        if (projectId != null) {
            requireProject(projectId, user.getOrganizationId());
            entities = operationRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                    user.getOrganizationId(), projectId);
        } else {
            entities = operationRepository.findByOrganizationIdOrderByCreatedAtDesc(user.getOrganizationId());
        }
        return entities.stream().map(storageService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Release get(UUID releaseId, AuthenticatedUser user) {
        authorizationService.require(user, ReleaseAuthorizationService.RELEASE_READ);
        requireEnabled();
        return storageService.toDto(storageService.requireForOrg(releaseId, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public Release getHistory(UUID releaseId, AuthenticatedUser user) {
        // History is the release DTO including full timeline events.
        return get(releaseId, user);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "RELEASE_DISABLED", "Release Manager is disabled");
        }
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RELEASE_INVALID_STATUS", "Project not found"));
    }

    private static List<UUID> normalizeIds(List<UUID> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return ids.stream().filter(Objects::nonNull).distinct().sorted().toList();
    }

    private static List<String> normalizeShas(List<String> shas) {
        if (shas == null || shas.isEmpty()) {
            return List.of();
        }
        return shas.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .distinct()
                .sorted()
                .toList();
    }

    private static List<ArtifactFingerprint> toFingerprints(List<ArtifactRef> artifacts) {
        if (artifacts == null || artifacts.isEmpty()) {
            return List.of();
        }
        List<ArtifactFingerprint> result = new ArrayList<>();
        for (ArtifactRef ref : artifacts) {
            if (ref == null) {
                continue;
            }
            result.add(new ArtifactFingerprint(ref.artifactType(), ref.artifactUri(), ref.artifactHash()));
        }
        return result;
    }

    private void publishAudit(
            AuthenticatedUser user,
            ReleaseOperationEntity entity,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        try {
            auditRecordingSupport.recordDomainEvent(
                    user,
                    entity.getProjectId(),
                    AuditEntityType.RELEASE,
                    entity.getId(),
                    entity.getReleaseName(),
                    action,
                    result,
                    AuditSource.RELEASE_MANAGER,
                    details);
        } catch (RuntimeException ignored) {
            // AuditPublisher swallows failures; guard against unexpected propagation.
        }
    }
}
