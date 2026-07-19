package ai.nova.platform.repair.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "repair_operations")
public class RepairOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RepairStatus status;

    @Column(name = "attempt_number", nullable = false)
    private int attemptNumber;

    @Column(name = "prior_patch_result_id", nullable = false)
    private UUID priorPatchResultId;

    @Column(name = "new_patch_result_id")
    private UUID newPatchResultId;

    @Column(nullable = false, length = 2000)
    private String reason;

    @Column(length = 4000)
    private String summary;

    @Column
    private Double confidence;

    @Column(name = "input_fingerprint", nullable = false, length = 64)
    private String inputFingerprint;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected RepairOperationEntity() {
    }

    public RepairOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID taskId,
            RepairStatus status,
            int attemptNumber,
            UUID priorPatchResultId,
            UUID newPatchResultId,
            String reason,
            String summary,
            Double confidence,
            String inputFingerprint,
            String errorCode,
            String errorMessage,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.taskId = taskId;
        this.status = status;
        this.attemptNumber = attemptNumber;
        this.priorPatchResultId = priorPatchResultId;
        this.newPatchResultId = newPatchResultId;
        this.reason = reason;
        this.summary = summary;
        this.confidence = confidence;
        this.inputFingerprint = inputFingerprint;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateStatus(RepairStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(
            UUID newPatchResultId, String summary, Double confidence, String reason, Instant completedAt) {
        this.status = RepairStatus.SUCCEEDED;
        this.newPatchResultId = newPatchResultId;
        this.summary = summary;
        this.confidence = confidence;
        this.reason = reason;
        this.errorCode = null;
        this.errorMessage = null;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }

    public void markFailed(String errorCode, String errorMessage, Instant completedAt) {
        this.status = RepairStatus.FAILED;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
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

    public RepairStatus getStatus() {
        return status;
    }

    public int getAttemptNumber() {
        return attemptNumber;
    }

    public UUID getPriorPatchResultId() {
        return priorPatchResultId;
    }

    public UUID getNewPatchResultId() {
        return newPatchResultId;
    }

    public String getReason() {
        return reason;
    }

    public String getSummary() {
        return summary;
    }

    public Double getConfidence() {
        return confidence;
    }

    public String getInputFingerprint() {
        return inputFingerprint;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
