package ai.nova.platform.rollback.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rollback_operations")
public class RollbackOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "release_operation_id", nullable = false)
    private UUID releaseOperationId;

    @Column(name = "deployment_operation_id", nullable = false)
    private UUID deploymentOperationId;

    @Column(name = "target_release_operation_id", nullable = false)
    private UUID targetReleaseOperationId;

    @Column(name = "current_version", nullable = false, length = 64)
    private String currentVersion;

    @Column(name = "target_version", nullable = false, length = 64)
    private String targetVersion;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "environment_code", nullable = false, length = 40)
    private String environmentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private RollbackStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 40)
    private RollbackStrategy strategy;

    @Column(name = "rollback_plan_hash", nullable = false, length = 64)
    private String rollbackPlanHash;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "validated_at")
    private Instant validatedAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    protected RollbackOperationEntity() {
    }

    public RollbackOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID releaseOperationId,
            UUID deploymentOperationId,
            UUID targetReleaseOperationId,
            String currentVersion,
            String targetVersion,
            UUID environmentId,
            String environmentCode,
            RollbackStatus status,
            RollbackStrategy strategy,
            String rollbackPlanHash,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.releaseOperationId = releaseOperationId;
        this.deploymentOperationId = deploymentOperationId;
        this.targetReleaseOperationId = targetReleaseOperationId;
        this.currentVersion = currentVersion;
        this.targetVersion = targetVersion;
        this.environmentId = environmentId;
        this.environmentCode = environmentCode;
        this.status = status;
        this.strategy = strategy;
        this.rollbackPlanHash = rollbackPlanHash;
        this.createdBy = createdBy;
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

    public UUID getDeploymentOperationId() {
        return deploymentOperationId;
    }

    public UUID getTargetReleaseOperationId() {
        return targetReleaseOperationId;
    }

    public String getCurrentVersion() {
        return currentVersion;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public String getEnvironmentCode() {
        return environmentCode;
    }

    public RollbackStatus getStatus() {
        return status;
    }

    public void setStatus(RollbackStatus status) {
        this.status = status;
    }

    public RollbackStrategy getStrategy() {
        return strategy;
    }

    public String getRollbackPlanHash() {
        return rollbackPlanHash;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getValidatedAt() {
        return validatedAt;
    }

    public void setValidatedAt(Instant validatedAt) {
        this.validatedAt = validatedAt;
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
