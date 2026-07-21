package ai.nova.platform.deployment.service;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.deployment.config.DeploymentProperties;
import ai.nova.platform.deployment.dto.DeploymentDtos.Deployment;
import ai.nova.platform.deployment.dto.DeploymentDtos.EnvironmentView;
import ai.nova.platform.deployment.dto.DeploymentDtos.ObserveDeploymentRequest;
import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.DeploymentEventType;
import ai.nova.platform.deployment.entity.DeploymentHealthLevel;
import ai.nova.platform.deployment.entity.DeploymentOperationEntity;
import ai.nova.platform.deployment.entity.DeploymentStatus;
import ai.nova.platform.deployment.entity.EnvironmentType;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.deployment.repository.DeploymentOperationRepository;
import ai.nova.platform.deployment.security.DeploymentAuthorizationService;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.repository.ReleaseOperationRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Deployment Observation records deployment state across environments.
 * Observe-only: never deploys, restarts, rolls back, or mutates releases.
 */
@Service
public class DeploymentObservationService {

    private final DeploymentProperties properties;
    private final DeploymentAuthorizationService authorizationService;
    private final DeploymentStorageService storageService;
    private final DeploymentOperationRepository operationRepository;
    private final DeploymentEnvironmentRepository environmentRepository;
    private final ReleaseOperationRepository releaseOperationRepository;
    private final AuditRecordingSupport auditRecordingSupport;

    public DeploymentObservationService(
            DeploymentProperties properties,
            DeploymentAuthorizationService authorizationService,
            DeploymentStorageService storageService,
            DeploymentOperationRepository operationRepository,
            DeploymentEnvironmentRepository environmentRepository,
            ReleaseOperationRepository releaseOperationRepository,
            AuditRecordingSupport auditRecordingSupport) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.storageService = storageService;
        this.operationRepository = operationRepository;
        this.environmentRepository = environmentRepository;
        this.releaseOperationRepository = releaseOperationRepository;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional
    public Deployment observe(ObserveDeploymentRequest request, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentAuthorizationService.DEPLOYMENT_RUN);
        requireEnabled();
        if (!properties.isAllowExternalEvents() && request.externalDeploymentKey() != null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DEPLOYMENT_INVALID_STATUS",
                    "External deployment events are disabled");
        }

        ReleaseOperationEntity release = releaseOperationRepository
                .findByIdAndOrganizationId(request.releaseId(), user.getOrganizationId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "DEPLOYMENT_RELEASE_NOT_FOUND", "Release not found for observation"));

        if (release.getStatus() != ReleaseStatus.PUBLISHED && release.getStatus() != ReleaseStatus.READY) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "DEPLOYMENT_INVALID_STATUS",
                    "Release must be READY or PUBLISHED to observe deployment; was " + release.getStatus());
        }

        DeploymentEnvironmentEntity environment = resolveEnvironment(request.environment());
        if (environment.getEnvironmentType() == EnvironmentType.CUSTOM
                && (request.customEnvironmentName() == null || request.customEnvironmentName().isBlank())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DEPLOYMENT_ENVIRONMENT_UNKNOWN",
                    "customEnvironmentName is required for CUSTOM environment");
        }

        DeploymentStatus status = request.status() != null ? request.status() : DeploymentStatus.PENDING;
        DeploymentHealthLevel health = request.health() != null ? request.health() : DeploymentHealthLevel.UNKNOWN;
        Instant startedAt = request.startedAt() != null ? request.startedAt() : Instant.now();

        String hash = DeploymentStorageService.deploymentHash(
                release.getId(),
                environment.getCode(),
                request.externalDeploymentKey(),
                request.deploymentProvider(),
                startedAt);

        var existing = operationRepository.findByOrganizationIdAndDeploymentHash(user.getOrganizationId(), hash);
        if (existing.isPresent()) {
            storageService.appendEvent(
                    existing.get().getId(),
                    DeploymentEventType.IDEMPOTENT_RETURN,
                    "Identical deployment observation already exists",
                    Instant.now());
            return storageService.toDto(existing.get());
        }

        DeploymentOperationEntity created = storageService.createObserved(
                user.getOrganizationId(),
                release.getProjectId(),
                release.getId(),
                environment,
                request.customEnvironmentName(),
                release.getSemanticVersion(),
                release.getManifestHash(),
                status,
                health,
                request.healthMessage(),
                request.deploymentProvider().trim(),
                request.externalDeploymentKey(),
                hash,
                user.getUserId(),
                startedAt,
                request.finishedAt(),
                request.logMetadata(),
                request.artifacts());
        auditRecordingSupport.recordDomainEvent(
                user,
                release.getProjectId(),
                AuditEntityType.DEPLOYMENT,
                created.getId(),
                created.getExternalDeploymentKey(),
                AuditAction.OBSERVE,
                AuditResult.SUCCESS,
                AuditSource.DEPLOYMENT_OBSERVATION,
                Map.of("releaseId", release.getId().toString(), "environment", environment.getCode()));
        return storageService.toDto(created);
    }

    @Transactional
    public Deployment verify(UUID deploymentId, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentAuthorizationService.DEPLOYMENT_RUN);
        requireEnabled();
        DeploymentOperationEntity entity = storageService.requireForOrg(deploymentId, user.getOrganizationId());

        if (entity.getStatus() == DeploymentStatus.SUCCEEDED) {
            return storageService.toDto(entity);
        }
        if (entity.getStatus() == DeploymentStatus.CANCELLED) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "DEPLOYMENT_INVALID_STATUS", "Cannot verify a cancelled deployment");
        }

        Instant now = Instant.now();
        DeploymentStatus priorStatus = entity.getStatus();
        DeploymentHealthLevel priorHealth = entity.getHealth();
        storageService.appendEvent(deploymentId, DeploymentEventType.VERIFY_STARTED, "Verification started", now);
        entity.setStatus(DeploymentStatus.VERIFYING);
        entity.setUpdatedAt(now);
        operationRepository.save(entity);

        ReleaseOperationEntity release = releaseOperationRepository
                .findByIdAndOrganizationId(entity.getReleaseOperationId(), user.getOrganizationId())
                .orElse(null);
        if (release == null) {
            failVerification(entity, "Linked release not found");
        }

        if (entity.getReleaseManifestHash() != null
                && release.getManifestHash() != null
                && !entity.getReleaseManifestHash().equals(release.getManifestHash())) {
            failVerification(entity, "Release manifest hash mismatch");
        }
        if (priorHealth == DeploymentHealthLevel.FAILED || priorHealth == DeploymentHealthLevel.DEGRADED) {
            failVerification(entity, "Observed health is " + priorHealth);
        }
        if (priorStatus == DeploymentStatus.FAILED) {
            failVerification(entity, "Observed status is FAILED");
        }

        DeploymentHealthLevel health = priorHealth == DeploymentHealthLevel.UNKNOWN
                ? DeploymentHealthLevel.HEALTHY
                : priorHealth;
        DeploymentOperationEntity verified =
                storageService.updateStatus(deploymentId, DeploymentStatus.SUCCEEDED, health, entity.getHealthMessage());
        storageService.appendEvent(deploymentId, DeploymentEventType.VERIFY_PASSED, "Verification passed", Instant.now());
        storageService.appendEvent(
                deploymentId, DeploymentEventType.COMPLETED, "Deployment observation completed", Instant.now());
        return storageService.toDto(verified);
    }

    @Transactional(readOnly = true)
    public List<Deployment> list(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentAuthorizationService.DEPLOYMENT_READ);
        requireEnabled();
        List<DeploymentOperationEntity> entities = projectId == null
                ? operationRepository.findByOrganizationIdOrderByCreatedAtDesc(user.getOrganizationId())
                : operationRepository.findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
                        user.getOrganizationId(), projectId);
        return entities.stream().map(storageService::toDto).toList();
    }

    @Transactional(readOnly = true)
    public Deployment get(UUID id, AuthenticatedUser user) {
        authorizationService.require(user, DeploymentAuthorizationService.DEPLOYMENT_READ);
        requireEnabled();
        return storageService.toDto(storageService.requireForOrg(id, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public Deployment history(UUID id, AuthenticatedUser user) {
        return get(id, user);
    }

    @Transactional(readOnly = true)
    public List<EnvironmentView> listEnvironments(AuthenticatedUser user) {
        authorizationService.require(user, DeploymentAuthorizationService.DEPLOYMENT_READ);
        requireEnabled();
        return environmentRepository.findByActiveTrueOrderBySortOrderAsc().stream()
                .map(e -> new EnvironmentView(
                        e.getId(), e.getCode(), e.getName(), e.getEnvironmentType(), e.getSortOrder(), e.isActive()))
                .toList();
    }

    private DeploymentEnvironmentEntity resolveEnvironment(String code) {
        if (code == null || code.isBlank()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "DEPLOYMENT_ENVIRONMENT_UNKNOWN", "Environment is required");
        }
        return environmentRepository
                .findByCodeIgnoreCase(code.trim())
                .filter(DeploymentEnvironmentEntity::isActive)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "DEPLOYMENT_ENVIRONMENT_UNKNOWN",
                        "Unknown environment: " + code.toUpperCase(Locale.ROOT)));
    }

    private void failVerification(DeploymentOperationEntity entity, String message) {
        Instant now = Instant.now();
        entity.setStatus(DeploymentStatus.FAILED);
        entity.setHealth(DeploymentHealthLevel.FAILED);
        entity.setHealthMessage(message);
        entity.setErrorCode("DEPLOYMENT_VERIFICATION_FAILED");
        entity.setErrorMessage(message);
        entity.setFinishedAt(now);
        entity.setDurationMs(DeploymentStorageService.computeDuration(entity.getStartedAt(), now));
        entity.setUpdatedAt(now);
        operationRepository.save(entity);
        storageService.appendEvent(entity.getId(), DeploymentEventType.VERIFY_FAILED, message, now);
        storageService.appendHealth(entity.getId(), DeploymentHealthLevel.FAILED, message, now);
        throw new ApiException(HttpStatus.CONFLICT, "DEPLOYMENT_VERIFICATION_FAILED", message);
    }

    private void requireEnabled() {
        if (!properties.isEnabled()) {
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE, "DEPLOYMENT_DISABLED", "Deployment Observation is disabled");
        }
        if (!properties.isObserveOnly()) {
            // Hard safety: this component must remain observe-only in Phase 2.
            throw new ApiException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "DEPLOYMENT_DISABLED",
                    "Deployment Observation must remain observe-only");
        }
    }
}
