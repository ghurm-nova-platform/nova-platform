package ai.nova.platform.deployment.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.deployment.dto.DeploymentDtos.ArtifactItem;
import ai.nova.platform.deployment.dto.DeploymentDtos.ArtifactRef;
import ai.nova.platform.deployment.dto.DeploymentDtos.Deployment;
import ai.nova.platform.deployment.dto.DeploymentDtos.HealthSnapshot;
import ai.nova.platform.deployment.dto.DeploymentDtos.TimelineEvent;
import ai.nova.platform.deployment.entity.DeploymentArtifactEntity;
import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.DeploymentEventEntity;
import ai.nova.platform.deployment.entity.DeploymentEventType;
import ai.nova.platform.deployment.entity.DeploymentHealthEntity;
import ai.nova.platform.deployment.entity.DeploymentHealthLevel;
import ai.nova.platform.deployment.entity.DeploymentOperationEntity;
import ai.nova.platform.deployment.entity.DeploymentStatus;
import ai.nova.platform.deployment.repository.DeploymentArtifactRepository;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.deployment.repository.DeploymentEventRepository;
import ai.nova.platform.deployment.repository.DeploymentHealthRepository;
import ai.nova.platform.deployment.repository.DeploymentOperationRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class DeploymentStorageService {

    private final DeploymentOperationRepository operationRepository;
    private final DeploymentEventRepository eventRepository;
    private final DeploymentHealthRepository healthRepository;
    private final DeploymentArtifactRepository artifactRepository;
    private final DeploymentEnvironmentRepository environmentRepository;

    public DeploymentStorageService(
            DeploymentOperationRepository operationRepository,
            DeploymentEventRepository eventRepository,
            DeploymentHealthRepository healthRepository,
            DeploymentArtifactRepository artifactRepository,
            DeploymentEnvironmentRepository environmentRepository) {
        this.operationRepository = operationRepository;
        this.eventRepository = eventRepository;
        this.healthRepository = healthRepository;
        this.artifactRepository = artifactRepository;
        this.environmentRepository = environmentRepository;
    }

    @Transactional
    public DeploymentOperationEntity createObserved(
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            DeploymentEnvironmentEntity environment,
            String customEnvironmentName,
            String semanticVersion,
            String releaseManifestHash,
            DeploymentStatus status,
            DeploymentHealthLevel health,
            String healthMessage,
            String deploymentProvider,
            String externalDeploymentKey,
            String deploymentHash,
            UUID triggeredBy,
            Instant startedAt,
            Instant finishedAt,
            String logMetadata,
            List<ArtifactRef> artifacts) {
        Instant now = Instant.now();
        Long durationMs = computeDuration(startedAt, finishedAt);
        UUID id = UUID.randomUUID();
        DeploymentOperationEntity entity = new DeploymentOperationEntity(
                id,
                organizationId,
                projectId,
                releaseId,
                environment.getId(),
                truncate(customEnvironmentName, 100),
                semanticVersion,
                releaseManifestHash,
                status,
                health,
                truncate(healthMessage, 2000),
                deploymentProvider,
                truncate(externalDeploymentKey, 255),
                deploymentHash,
                triggeredBy,
                startedAt,
                finishedAt,
                durationMs,
                truncate(logMetadata, 2000),
                now);
        operationRepository.save(entity);
        appendEvent(id, DeploymentEventType.OBSERVED, "Deployment observation recorded", now);
        appendHealth(id, health, healthMessage, now);
        if (artifacts != null) {
            for (ArtifactRef ref : artifacts) {
                if (ref == null) {
                    continue;
                }
                artifactRepository.save(new DeploymentArtifactEntity(
                        UUID.randomUUID(),
                        id,
                        ref.artifactType(),
                        truncate(ref.artifactUri(), 2000),
                        truncate(ref.artifactHash(), 64),
                        truncate(ref.label(), 255),
                        now));
            }
        }
        return entity;
    }

    @Transactional
    public DeploymentOperationEntity updateStatus(
            UUID deploymentId, DeploymentStatus status, DeploymentHealthLevel health, String healthMessage) {
        DeploymentOperationEntity entity = require(deploymentId);
        Instant now = Instant.now();
        DeploymentStatus previous = entity.getStatus();
        entity.setStatus(status);
        entity.setHealth(health);
        entity.setHealthMessage(truncate(healthMessage, 2000));
        entity.setUpdatedAt(now);
        if (status == DeploymentStatus.SUCCEEDED
                || status == DeploymentStatus.FAILED
                || status == DeploymentStatus.CANCELLED) {
            if (entity.getFinishedAt() == null) {
                entity.setFinishedAt(now);
            }
            entity.setDurationMs(computeDuration(entity.getStartedAt(), entity.getFinishedAt()));
        }
        operationRepository.save(entity);
        if (previous != status) {
            appendEvent(deploymentId, DeploymentEventType.STATUS_CHANGED, previous + " -> " + status, now);
        }
        appendEvent(deploymentId, DeploymentEventType.HEALTH_CHANGED, "Health=" + health, now);
        appendHealth(deploymentId, health, healthMessage, now);
        return entity;
    }

    @Transactional
    public void appendEvent(UUID deploymentId, DeploymentEventType type, String detail, Instant at) {
        eventRepository.save(new DeploymentEventEntity(UUID.randomUUID(), deploymentId, type, truncate(detail, 2000), at));
    }

    @Transactional
    public void appendHealth(UUID deploymentId, DeploymentHealthLevel health, String message, Instant observedAt) {
        healthRepository.save(new DeploymentHealthEntity(
                UUID.randomUUID(), deploymentId, health, truncate(message, 2000), observedAt, Instant.now()));
    }

    public DeploymentOperationEntity require(UUID id) {
        return operationRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEPLOYMENT_NOT_FOUND", "Deployment not found"));
    }

    public DeploymentOperationEntity requireForOrg(UUID id, UUID organizationId) {
        return operationRepository.findByIdAndOrganizationId(id, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "DEPLOYMENT_NOT_FOUND", "Deployment not found"));
    }

    public Deployment toDto(DeploymentOperationEntity entity) {
        DeploymentEnvironmentEntity environment = environmentRepository
                .findById(entity.getEnvironmentId())
                .orElseThrow(() -> new ApiException(
                        HttpStatus.INTERNAL_SERVER_ERROR, "DEPLOYMENT_ENVIRONMENT_UNKNOWN", "Environment missing"));
        List<DeploymentArtifactEntity> artifacts =
                artifactRepository.findByDeploymentOperationIdOrderByCreatedAtAsc(entity.getId());
        List<DeploymentHealthEntity> healthHistory =
                healthRepository.findByDeploymentOperationIdOrderByObservedAtDesc(entity.getId());
        List<DeploymentEventEntity> events =
                eventRepository.findByDeploymentOperationIdOrderByCreatedAtAsc(entity.getId());

        List<ArtifactItem> artifactItems = new ArrayList<>();
        for (DeploymentArtifactEntity a : artifacts) {
            artifactItems.add(new ArtifactItem(
                    a.getId(), a.getArtifactType(), a.getArtifactUri(), a.getArtifactHash(), a.getLabel(), a.getCreatedAt()));
        }
        List<HealthSnapshot> healthItems = new ArrayList<>();
        for (DeploymentHealthEntity h : healthHistory) {
            healthItems.add(new HealthSnapshot(h.getId(), h.getHealth(), h.getMessage(), h.getObservedAt()));
        }
        List<TimelineEvent> timeline = events.stream()
                .map(e -> new TimelineEvent(e.getEventType().name(), e.getCreatedAt(), e.getDetail()))
                .toList();

        return new Deployment(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getProjectId(),
                entity.getReleaseOperationId(),
                entity.getEnvironmentId(),
                environment.getCode(),
                environment.getName(),
                entity.getCustomEnvironmentName(),
                entity.getSemanticVersion(),
                entity.getReleaseManifestHash(),
                entity.getStatus(),
                entity.getHealth(),
                entity.getHealthMessage(),
                entity.getDeploymentProvider(),
                entity.getExternalDeploymentKey(),
                entity.getDeploymentHash(),
                entity.getTriggeredBy(),
                entity.getStartedAt(),
                entity.getFinishedAt(),
                entity.getDurationMs(),
                entity.getLogMetadata(),
                entity.getErrorCode(),
                entity.getErrorMessage(),
                artifactItems,
                healthItems,
                timeline,
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public static String deploymentHash(
            UUID releaseId, String environmentCode, String externalKey, String provider, Instant startedAt) {
        String raw = String.join(
                "|",
                releaseId.toString(),
                environmentCode == null ? "" : environmentCode.toUpperCase(),
                externalKey == null ? "" : externalKey.trim(),
                provider == null ? "" : provider.trim(),
                startedAt == null ? "" : startedAt.toString());
        return sha256(raw);
    }

    public static Long computeDuration(Instant startedAt, Instant finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return null;
        }
        if (finishedAt.isBefore(startedAt)) {
            return 0L;
        }
        return Duration.between(startedAt, finishedAt).toMillis();
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        return value.length() <= max ? value : value.substring(0, max);
    }

    private static String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }
}
