package ai.nova.platform.deployment.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deployment_operations")
public class DeploymentOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "release_operation_id", nullable = false)
    private UUID releaseOperationId;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "custom_environment_name", length = 100)
    private String customEnvironmentName;

    @Column(name = "semantic_version", nullable = false, length = 64)
    private String semanticVersion;

    @Column(name = "release_manifest_hash", length = 64)
    private String releaseManifestHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private DeploymentStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "health", nullable = false, length = 30)
    private DeploymentHealthLevel health;

    @Column(name = "health_message", length = 2000)
    private String healthMessage;

    @Column(name = "deployment_provider", nullable = false, length = 80)
    private String deploymentProvider;

    @Column(name = "external_deployment_key", length = 255)
    private String externalDeploymentKey;

    @Column(name = "deployment_hash", nullable = false, length = 64)
    private String deploymentHash;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "log_metadata", length = 2000)
    private String logMetadata;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected DeploymentOperationEntity() {
    }

    public DeploymentOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID releaseOperationId,
            UUID environmentId,
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
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.releaseOperationId = releaseOperationId;
        this.environmentId = environmentId;
        this.customEnvironmentName = customEnvironmentName;
        this.semanticVersion = semanticVersion;
        this.releaseManifestHash = releaseManifestHash;
        this.status = status;
        this.health = health;
        this.healthMessage = healthMessage;
        this.deploymentProvider = deploymentProvider;
        this.externalDeploymentKey = externalDeploymentKey;
        this.deploymentHash = deploymentHash;
        this.triggeredBy = triggeredBy;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
        this.durationMs = durationMs;
        this.logMetadata = logMetadata;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getReleaseOperationId() {
        return releaseOperationId;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public String getCustomEnvironmentName() {
        return customEnvironmentName;
    }

    public String getSemanticVersion() {
        return semanticVersion;
    }

    public String getReleaseManifestHash() {
        return releaseManifestHash;
    }

    public DeploymentStatus getStatus() {
        return status;
    }

    public void setStatus(DeploymentStatus status) {
        this.status = status;
    }

    public DeploymentHealthLevel getHealth() {
        return health;
    }

    public void setHealth(DeploymentHealthLevel health) {
        this.health = health;
    }

    public String getHealthMessage() {
        return healthMessage;
    }

    public void setHealthMessage(String healthMessage) {
        this.healthMessage = healthMessage;
    }

    public String getDeploymentProvider() {
        return deploymentProvider;
    }

    public String getExternalDeploymentKey() {
        return externalDeploymentKey;
    }

    public String getDeploymentHash() {
        return deploymentHash;
    }

    public UUID getTriggeredBy() {
        return triggeredBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public String getLogMetadata() {
        return logMetadata;
    }

    public void setLogMetadata(String logMetadata) {
        this.logMetadata = logMetadata;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
