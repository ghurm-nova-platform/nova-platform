package ai.nova.platform.deploymentexecution.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deployment_executions")
public class DeploymentExecutionEntity {

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

    @Column(name = "deployment_observation_id")
    private UUID deploymentObservationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider", nullable = false, length = 40)
    private ExecutionProviderCode provider;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExecutionStatus status;

    @Column(name = "current_step", length = 80)
    private String currentStep;

    @Column(name = "current_stage", length = 80)
    private String currentStage;

    @Column(name = "release_manifest_hash", length = 64)
    private String releaseManifestHash;

    @Column(name = "release_content_fingerprint", length = 64)
    private String releaseContentFingerprint;

    @Column(name = "execution_fingerprint", nullable = false, length = 64)
    private String executionFingerprint;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "triggered_by")
    private UUID triggeredBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    protected DeploymentExecutionEntity() {
    }

    public DeploymentExecutionEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID releaseOperationId,
            UUID environmentId,
            UUID deploymentObservationId,
            ExecutionProviderCode provider,
            ExecutionStatus status,
            String releaseManifestHash,
            String releaseContentFingerprint,
            String executionFingerprint,
            UUID triggeredBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.releaseOperationId = releaseOperationId;
        this.environmentId = environmentId;
        this.deploymentObservationId = deploymentObservationId;
        this.provider = provider;
        this.status = status;
        this.releaseManifestHash = releaseManifestHash;
        this.releaseContentFingerprint = releaseContentFingerprint;
        this.executionFingerprint = executionFingerprint;
        this.triggeredBy = triggeredBy;
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

    public UUID getDeploymentObservationId() {
        return deploymentObservationId;
    }

    public void setDeploymentObservationId(UUID deploymentObservationId) {
        this.deploymentObservationId = deploymentObservationId;
    }

    public ExecutionProviderCode getProvider() {
        return provider;
    }

    public ExecutionStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStatus status) {
        this.status = status;
    }

    public String getCurrentStep() {
        return currentStep;
    }

    public void setCurrentStep(String currentStep) {
        this.currentStep = currentStep;
    }

    public String getCurrentStage() {
        return currentStage;
    }

    public void setCurrentStage(String currentStage) {
        this.currentStage = currentStage;
    }

    public String getReleaseManifestHash() {
        return releaseManifestHash;
    }

    public String getReleaseContentFingerprint() {
        return releaseContentFingerprint;
    }

    public String getExecutionFingerprint() {
        return executionFingerprint;
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

    public UUID getTriggeredBy() {
        return triggeredBy;
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
}
