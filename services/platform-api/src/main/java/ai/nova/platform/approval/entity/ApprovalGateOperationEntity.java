package ai.nova.platform.approval.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_gate_operations")
public class ApprovalGateOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "policy_version", nullable = false)
    private int policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalOperationStatus status;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private ApprovalDecisionValue decision;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApprovalGateOperationEntity() {
    }

    public ApprovalGateOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID taskId,
            UUID policyId,
            int policyVersion,
            ApprovalOperationStatus status,
            ApprovalDecisionValue decision,
            Instant startedAt,
            Instant completedAt,
            String errorCode,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.taskId = taskId;
        this.policyId = policyId;
        this.policyVersion = policyVersion;
        this.status = status;
        this.decision = decision;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
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

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public int getPolicyVersion() {
        return policyVersion;
    }

    public ApprovalOperationStatus getStatus() {
        return status;
    }

    public void updateStatus(ApprovalOperationStatus status) {
        this.status = status;
    }

    public ApprovalDecisionValue getDecision() {
        return decision;
    }

    public void setDecision(ApprovalDecisionValue decision) {
        this.decision = decision;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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
