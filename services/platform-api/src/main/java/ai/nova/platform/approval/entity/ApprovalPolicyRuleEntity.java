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
@Table(name = "approval_policy_rules")
public class ApprovalPolicyRuleEntity {

    @Id
    private UUID id;

    @Column(name = "approval_policy_id", nullable = false)
    private UUID approvalPolicyId;

    @Column(name = "rule_code", nullable = false, length = 80)
    private String ruleCode;

    @Column(name = "rule_type", nullable = false, length = 40)
    private String ruleType;

    @Column(nullable = false, length = 40)
    private String operator;

    @Column(name = "expected_value", length = 500)
    private String expectedValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ApprovalSeverity severity;

    @Column(nullable = false)
    private boolean blocking;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApprovalPolicyRuleEntity() {
    }

    public ApprovalPolicyRuleEntity(
            UUID id,
            UUID approvalPolicyId,
            String ruleCode,
            String ruleType,
            String operator,
            String expectedValue,
            ApprovalSeverity severity,
            boolean blocking,
            int displayOrder,
            Instant createdAt) {
        this.id = id;
        this.approvalPolicyId = approvalPolicyId;
        this.ruleCode = ruleCode;
        this.ruleType = ruleType;
        this.operator = operator;
        this.expectedValue = expectedValue;
        this.severity = severity;
        this.blocking = blocking;
        this.displayOrder = displayOrder;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApprovalPolicyId() {
        return approvalPolicyId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public String getRuleType() {
        return ruleType;
    }

    public String getOperator() {
        return operator;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public ApprovalSeverity getSeverity() {
        return severity;
    }

    public boolean isBlocking() {
        return blocking;
    }

    public int getDisplayOrder() {
        return displayOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
