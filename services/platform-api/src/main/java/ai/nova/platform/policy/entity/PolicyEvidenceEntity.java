package ai.nova.platform.policy.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "policy_evidence")
public class PolicyEvidenceEntity {

    @Id
    private UUID id;

    @Column(name = "policy_evaluation_id", nullable = false)
    private UUID policyEvaluationId;

    @Column(name = "evidence_key", nullable = false, length = 120)
    private String evidenceKey;

    @Column(name = "evidence_type", nullable = false, length = 60)
    private String evidenceType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "detail", length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PolicyEvidenceEntity() {
    }

    public PolicyEvidenceEntity(
            UUID id,
            UUID policyEvaluationId,
            String evidenceKey,
            String evidenceType,
            UUID referenceId,
            boolean passed,
            String detail,
            Instant createdAt) {
        this.id = id;
        this.policyEvaluationId = policyEvaluationId;
        this.evidenceKey = evidenceKey;
        this.evidenceType = evidenceType;
        this.referenceId = referenceId;
        this.passed = passed;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPolicyEvaluationId() {
        return policyEvaluationId;
    }

    public String getEvidenceKey() {
        return evidenceKey;
    }

    public String getEvidenceType() {
        return evidenceType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
