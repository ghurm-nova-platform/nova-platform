package ai.nova.platform.release.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.release.dto.ReleaseDtos.Release;
import ai.nova.platform.release.dto.ReleaseDtos.ReleaseArtifactItem;
import ai.nova.platform.release.dto.ReleaseDtos.ReleaseContentItem;
import ai.nova.platform.release.dto.ReleaseDtos.ReleaseVersionView;
import ai.nova.platform.release.dto.ReleaseDtos.TimelineEvent;
import ai.nova.platform.release.entity.ReleaseArtifactEntity;
import ai.nova.platform.release.entity.ReleaseContentEntity;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseEventEntity;
import ai.nova.platform.release.entity.ReleaseEventType;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.entity.ReleaseVersionEntity;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.repository.ReleaseArtifactRepository;
import ai.nova.platform.release.repository.ReleaseContentRepository;
import ai.nova.platform.release.repository.ReleaseEventRepository;
import ai.nova.platform.release.repository.ReleaseOperationRepository;
import ai.nova.platform.release.repository.ReleaseVersionRepository;
import ai.nova.platform.release.service.ReleaseManifestService.ManifestResult;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.web.error.ApiException;

@Service
public class ReleaseStorageService {

    private final ReleaseOperationRepository operationRepository;
    private final ReleaseVersionRepository versionRepository;
    private final ReleaseContentRepository contentRepository;
    private final ReleaseArtifactRepository artifactRepository;
    private final ReleaseEventRepository eventRepository;

    public ReleaseStorageService(
            ReleaseOperationRepository operationRepository,
            ReleaseVersionRepository versionRepository,
            ReleaseContentRepository contentRepository,
            ReleaseArtifactRepository artifactRepository,
            ReleaseEventRepository eventRepository) {
        this.operationRepository = operationRepository;
        this.versionRepository = versionRepository;
        this.contentRepository = contentRepository;
        this.artifactRepository = artifactRepository;
        this.eventRepository = eventRepository;
    }

    @Transactional
    public ReleaseOperationEntity createDraft(
            UUID organizationId,
            UUID projectId,
            String releaseName,
            String description,
            VersionStrategy strategy,
            VersionBump bumpType,
            ResolvedVersion version,
            String contentFingerprint,
            UUID createdBy,
            List<ContentSpec> contents,
            List<ArtifactSpec> artifacts) {
        Instant now = Instant.now();
        long nextNumber = operationRepository.findMaxReleaseNumber(organizationId, projectId) + 1;
        UUID releaseId = UUID.randomUUID();
        ReleaseOperationEntity entity = new ReleaseOperationEntity(
                releaseId,
                organizationId,
                projectId,
                nextNumber,
                version.semanticVersion(),
                releaseName,
                truncate(description, 2000),
                ReleaseStatus.DRAFT,
                strategy,
                bumpType,
                contentFingerprint,
                createdBy,
                now);
        operationRepository.save(entity);

        versionRepository.save(new ReleaseVersionEntity(
                UUID.randomUUID(),
                releaseId,
                organizationId,
                projectId,
                version.semanticVersion(),
                strategy,
                bumpType,
                version.major(),
                version.minor(),
                version.patch(),
                now));

        int order = 0;
        for (ContentSpec content : contents) {
            contentRepository.save(new ReleaseContentEntity(
                    UUID.randomUUID(),
                    releaseId,
                    content.type(),
                    content.referenceId(),
                    content.commitSha(),
                    order++,
                    now));
        }
        for (ArtifactSpec artifact : artifacts) {
            artifactRepository.save(new ReleaseArtifactEntity(
                    UUID.randomUUID(),
                    releaseId,
                    artifact.type(),
                    truncate(artifact.uri(), 2000),
                    truncate(artifact.hash(), 64),
                    truncate(artifact.label(), 255),
                    now));
        }
        appendEvent(releaseId, ReleaseEventType.CREATED, "Release draft created", now);
        return entity;
    }

    @Transactional
    public ReleaseOperationEntity markPreparing(UUID releaseId) {
        ReleaseOperationEntity entity = require(releaseId);
        entity.setStatus(ReleaseStatus.PREPARING);
        entity.setUpdatedAt(Instant.now());
        appendEvent(releaseId, ReleaseEventType.PREPARE_STARTED, "Preparing release manifest", Instant.now());
        return operationRepository.save(entity);
    }

    @Transactional
    public ReleaseOperationEntity markReady(UUID releaseId, ManifestResult manifest) {
        ReleaseOperationEntity entity = require(releaseId);
        Instant now = Instant.now();
        entity.setStatus(ReleaseStatus.READY);
        entity.setManifestJson(manifest.manifestJson());
        entity.setManifestHash(manifest.manifestHash());
        entity.setPreparedAt(now);
        entity.setErrorCode(null);
        entity.setErrorMessage(null);
        entity.setUpdatedAt(now);
        operationRepository.save(entity);
        appendEvent(releaseId, ReleaseEventType.MANIFEST_GENERATED, "Manifest hash=" + manifest.manifestHash(), now);
        appendEvent(releaseId, ReleaseEventType.READY, "Release ready and immutable", now);
        return entity;
    }

    @Transactional
    public ReleaseOperationEntity markPublished(UUID releaseId) {
        ReleaseOperationEntity entity = require(releaseId);
        Instant now = Instant.now();
        entity.setStatus(ReleaseStatus.PUBLISHED);
        entity.setPublishedAt(now);
        entity.setUpdatedAt(now);
        operationRepository.save(entity);
        appendEvent(releaseId, ReleaseEventType.PUBLISHED, "Release published", now);
        return entity;
    }

    @Transactional
    public ReleaseOperationEntity markFailed(UUID releaseId, String errorCode, String errorMessage) {
        ReleaseOperationEntity entity = require(releaseId);
        Instant now = Instant.now();
        entity.setStatus(ReleaseStatus.FAILED);
        entity.setErrorCode(errorCode);
        entity.setErrorMessage(truncate(errorMessage, 2000));
        entity.setUpdatedAt(now);
        operationRepository.save(entity);
        appendEvent(releaseId, ReleaseEventType.FAILED, truncate(errorMessage, 2000), now);
        return entity;
    }

    @Transactional
    public void appendEvent(UUID releaseId, ReleaseEventType type, String detail, Instant at) {
        eventRepository.save(new ReleaseEventEntity(UUID.randomUUID(), releaseId, type, truncate(detail, 2000), at));
    }

    public ReleaseOperationEntity require(UUID releaseId) {
        return operationRepository.findById(releaseId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RELEASE_INVALID_STATUS", "Release not found"));
    }

    public ReleaseOperationEntity requireForOrg(UUID releaseId, UUID organizationId) {
        return operationRepository.findByIdAndOrganizationId(releaseId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "RELEASE_INVALID_STATUS", "Release not found"));
    }

    public Release toDto(ReleaseOperationEntity entity) {
        List<ReleaseContentEntity> contents =
                contentRepository.findByReleaseOperationIdOrderBySortOrderAsc(entity.getId());
        List<ReleaseArtifactEntity> artifacts =
                artifactRepository.findByReleaseOperationIdOrderByCreatedAtAsc(entity.getId());
        ReleaseVersionEntity version = versionRepository.findByReleaseOperationId(entity.getId()).orElse(null);
        List<ReleaseEventEntity> events =
                eventRepository.findByReleaseOperationIdOrderByCreatedAtAsc(entity.getId());
        return map(entity, contents, artifacts, version, events);
    }

    public List<ReleaseContentEntity> contents(UUID releaseId) {
        return contentRepository.findByReleaseOperationIdOrderBySortOrderAsc(releaseId);
    }

    public List<ReleaseArtifactEntity> artifacts(UUID releaseId) {
        return artifactRepository.findByReleaseOperationIdOrderByCreatedAtAsc(releaseId);
    }

    private Release map(
            ReleaseOperationEntity entity,
            List<ReleaseContentEntity> contents,
            List<ReleaseArtifactEntity> artifacts,
            ReleaseVersionEntity version,
            List<ReleaseEventEntity> events) {
        List<ReleaseContentItem> contentItems = new ArrayList<>();
        for (ReleaseContentEntity c : contents) {
            contentItems.add(new ReleaseContentItem(
                    c.getId(), c.getContentType(), c.getReferenceId(), c.getCommitSha(), c.getSortOrder()));
        }
        List<ReleaseArtifactItem> artifactItems = new ArrayList<>();
        for (ReleaseArtifactEntity a : artifacts) {
            artifactItems.add(new ReleaseArtifactItem(
                    a.getId(), a.getArtifactType(), a.getArtifactUri(), a.getArtifactHash(), a.getLabel(), a.getCreatedAt()));
        }
        ReleaseVersionView versionView = version == null
                ? null
                : new ReleaseVersionView(
                        version.getId(),
                        version.getSemanticVersion(),
                        version.getVersionStrategy(),
                        version.getBumpType(),
                        version.getMajorVersion(),
                        version.getMinorVersion(),
                        version.getPatchVersion(),
                        version.getCreatedAt());
        List<TimelineEvent> timeline = events.stream()
                .map(e -> new TimelineEvent(e.getEventType().name(), e.getCreatedAt(), e.getDetail()))
                .toList();
        return new Release(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getReleaseNumber(),
                entity.getSemanticVersion(),
                entity.getReleaseName(),
                entity.getDescription(),
                entity.getStatus(),
                entity.getVersionStrategy(),
                entity.getBumpType(),
                entity.getContentFingerprint(),
                entity.getManifestHash(),
                entity.getManifestJson(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                entity.getCreatedBy(),
                contentItems,
                artifactItems,
                versionView,
                timeline,
                entity.getPreparedAt(),
                entity.getPublishedAt(),
                entity.getArchivedAt(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    public record ContentSpec(ReleaseContentType type, UUID referenceId, String commitSha) {
    }

    public record ArtifactSpec(String type, String uri, String hash, String label) {
    }
}
