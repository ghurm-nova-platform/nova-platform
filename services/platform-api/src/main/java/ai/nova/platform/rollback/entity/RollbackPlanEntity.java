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
@Table(name = "rollback_plans")
public class RollbackPlanEntity {

    @Id
    private UUID id;

    @Column(name = "rollback_operation_id", nullable = false)
    private UUID rollbackOperationId;

    @Column(name = "current_release_operation_id", nullable = false)
    private UUID currentReleaseOperationId;

    @Column(name = "target_release_operation_id", nullable = false)
    private UUID targetReleaseOperationId;

    @Column(name = "deployment_operation_id", nullable = false)
    private UUID deploymentOperationId;

    @Column(name = "environment_code", nullable = false, length = 40)
    private String environmentCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "strategy", nullable = false, length = 40)
    private RollbackStrategy strategy;

    @Column(name = "reason", length = 2000)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false, length = 30)
    private RollbackRiskLevel riskLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "validation_result", nullable = false, length = 30)
    private RollbackValidationResult validationResult;

    @Column(name = "validation_message", length = 2000)
    private String validationMessage;

    @Column(name = "plan_json", nullable = false, columnDefinition = "TEXT")
    private String planJson;

    @Column(name = "immutable", nullable = false)
    private boolean immutable;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RollbackPlanEntity() {
    }

    public RollbackPlanEntity(
            UUID id,
            UUID rollbackOperationId,
            UUID currentReleaseOperationId,
            UUID targetReleaseOperationId,
            UUID deploymentOperationId,
            String environmentCode,
            RollbackStrategy strategy,
            String reason,
            RollbackRiskLevel riskLevel,
            RollbackValidationResult validationResult,
            String planJson,
            boolean immutable,
            Instant createdAt) {
        this.id = id;
        this.rollbackOperationId = rollbackOperationId;
        this.currentReleaseOperationId = currentReleaseOperationId;
        this.targetReleaseOperationId = targetReleaseOperationId;
        this.deploymentOperationId = deploymentOperationId;
        this.environmentCode = environmentCode;
        this.strategy = strategy;
        this.reason = reason;
        this.riskLevel = riskLevel;
        this.validationResult = validationResult;
        this.planJson = planJson;
        this.immutable = immutable;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRollbackOperationId() {
        return rollbackOperationId;
    }

    public UUID getCurrentReleaseOperationId() {
        return currentReleaseOperationId;
    }

    public UUID getTargetReleaseOperationId() {
        return targetReleaseOperationId;
    }

    public UUID getDeploymentOperationId() {
        return deploymentOperationId;
    }

    public String getEnvironmentCode() {
        return environmentCode;
    }

    public RollbackStrategy getStrategy() {
        return strategy;
    }

    public String getReason() {
        return reason;
    }

    public RollbackRiskLevel getRiskLevel() {
        return riskLevel;
    }

    public RollbackValidationResult getValidationResult() {
        return validationResult;
    }

    public void setValidationResult(RollbackValidationResult validationResult) {
        this.validationResult = validationResult;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public void setValidationMessage(String validationMessage) {
        this.validationMessage = validationMessage;
    }

    public String getPlanJson() {
        return planJson;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
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
