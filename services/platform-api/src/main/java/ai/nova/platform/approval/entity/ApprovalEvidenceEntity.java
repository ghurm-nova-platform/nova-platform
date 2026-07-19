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
@Table(name = "approval_evidence")
public class ApprovalEvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "approval_decision_id", nullable = false)
    private UUID approvalDecisionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "evidence_type", nullable = false, length = 40)
    private ApprovalEvidenceType evidenceType;

    @Column(name = "source_operation_id", nullable = false)
    private UUID sourceOperationId;

    @Column(name = "source_result_id")
    private UUID sourceResultId;

    @Column(name = "source_version", length = 80)
    private String sourceVersion;

    @Column(name = "source_hash", length = 128)
    private String sourceHash;

    @Column(name = "observed_status", length = 80)
    private String observedStatus;

    @Column(name = "observed_value", length = 2000)
    private String observedValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApprovalEvidenceEntity() {
    }

    public ApprovalEvidenceEntity(
            UUID id,
            UUID approvalDecisionId,
            ApprovalEvidenceType evidenceType,
            UUID sourceOperationId,
            UUID sourceResultId,
            String sourceVersion,
            String sourceHash,
            String observedStatus,
            String observedValue,
            Instant createdAt) {
        this.id = id;
        this.approvalDecisionId = approvalDecisionId;
        this.evidenceType = evidenceType;
        this.sourceOperationId = sourceOperationId;
        this.sourceResultId = sourceResultId;
        this.sourceVersion = sourceVersion;
        this.sourceHash = sourceHash;
        this.observedStatus = observedStatus;
        this.observedValue = observedValue;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApprovalDecisionId() {
        return approvalDecisionId;
    }

    public ApprovalEvidenceType getEvidenceType() {
        return evidenceType;
    }

    public UUID getSourceOperationId() {
        return sourceOperationId;
    }

    public UUID getSourceResultId() {
        return sourceResultId;
    }

    public String getSourceVersion() {
        return sourceVersion;
    }

    public String getSourceHash() {
        return sourceHash;
    }

    public String getObservedStatus() {
        return observedStatus;
    }

    public String getObservedValue() {
        return observedValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
