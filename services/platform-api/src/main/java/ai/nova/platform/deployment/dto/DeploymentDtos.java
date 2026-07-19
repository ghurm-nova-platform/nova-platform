package ai.nova.platform.deployment.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.deployment.entity.DeploymentHealthLevel;
import ai.nova.platform.deployment.entity.DeploymentStatus;
import ai.nova.platform.deployment.entity.EnvironmentType;

public final class DeploymentDtos {

    private DeploymentDtos() {
    }

    public record ArtifactRef(
            @NotBlank String artifactType,
            @NotBlank @Size(max = 2000) String artifactUri,
            @Size(max = 64) String artifactHash,
            @Size(max = 255) String label) {
    }

    public record ObserveDeploymentRequest(
            @NotNull UUID releaseId,
            @NotBlank @Size(max = 40) String environment,
            @Size(max = 100) String customEnvironmentName,
            DeploymentStatus status,
            DeploymentHealthLevel health,
            @Size(max = 2000) String healthMessage,
            @NotBlank @Size(max = 80) String deploymentProvider,
            @Size(max = 255) String externalDeploymentKey,
            Instant startedAt,
            Instant finishedAt,
            @Size(max = 2000) String logMetadata,
            List<ArtifactRef> artifacts) {
    }

    public record TimelineEvent(String eventType, Instant at, String detail) {
    }

    public record EnvironmentView(
            UUID id,
            String code,
            String name,
            EnvironmentType environmentType,
            int sortOrder,
            boolean active) {
    }

    public record HealthSnapshot(
            UUID id,
            DeploymentHealthLevel health,
            String message,
            Instant observedAt) {
    }

    public record ArtifactItem(
            UUID id,
            String artifactType,
            String artifactUri,
            String artifactHash,
            String label,
            Instant createdAt) {
    }

    public record Deployment(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID releaseId,
            UUID environmentId,
            String environmentCode,
            String environmentName,
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
            Long durationMs,
            String logMetadata,
            String errorCode,
            String errorMessage,
            List<ArtifactItem> artifacts,
            List<HealthSnapshot> healthHistory,
            List<TimelineEvent> timeline,
            Instant createdAt,
            Instant updatedAt) {
    }
}
