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
@Table(name = "approval_requirements")
public class ApprovalRequirementEntity {

    @Id
    private UUID id;

    @Column(name = "approval_decision_id", nullable = false)
    private UUID approvalDecisionId;

    @Column(name = "rule_code", nullable = false, length = 80)
    private String ruleCode;

    @Column(length = 1000)
    private String description;

    @Column(name = "expected_value", length = 500)
    private String expectedValue;

    @Column(name = "actual_value", length = 2000)
    private String actualValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalRequirementResult result;

    @Column(nullable = false)
    private boolean blocking;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalSeverity severity;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected ApprovalRequirementEntity() {
    }

    public ApprovalRequirementEntity(
            UUID id,
            UUID approvalDecisionId,
            String ruleCode,
            String description,
            String expectedValue,
            String actualValue,
            ApprovalRequirementResult result,
            boolean blocking,
            ApprovalSeverity severity,
            String failureReason,
            Instant evaluatedAt) {
        this.id = id;
        this.approvalDecisionId = approvalDecisionId;
        this.ruleCode = ruleCode;
        this.description = description;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.result = result;
        this.blocking = blocking;
        this.severity = severity;
        this.failureReason = failureReason;
        this.evaluatedAt = evaluatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApprovalDecisionId() {
        return approvalDecisionId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getDescription() {
        return description;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public ApprovalRequirementResult getResult() {
        return result;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public ApprovalSeverity getSeverity() {
        return severity;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }
}
