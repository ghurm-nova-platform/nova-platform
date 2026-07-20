package ai.nova.platform.policy.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "policy_evaluations")
public class PolicyEvaluationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "policy_version_id", nullable = false)
    private UUID policyVersionId;

    @Column(name = "release_operation_id", nullable = false)
    private UUID releaseOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision", nullable = false, length = 30)
    private PolicyDecision decision;

    @Column(name = "evaluation_hash", nullable = false, length = 64)
    private String evaluationHash;

    @Column(name = "summary", length = 2000)
    private String summary;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    @Column(name = "completed", nullable = false)
    private boolean completed;

    @Column(name = "evaluated_by")
    private UUID evaluatedBy;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PolicyEvaluationEntity() {
    }

    public PolicyEvaluationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID policyId,
            UUID policyVersionId,
            UUID releaseOperationId,
            PolicyDecision decision,
            String evaluationHash,
            String summary,
            boolean completed,
            UUID evaluatedBy,
            Instant evaluatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.policyId = policyId;
        this.policyVersionId = policyVersionId;
        this.releaseOperationId = releaseOperationId;
        this.decision = decision;
        this.evaluationHash = evaluationHash;
        this.summary = summary;
        this.completed = completed;
        this.evaluatedBy = evaluatedBy;
        this.evaluatedAt = evaluatedAt;
        this.createdAt = evaluatedAt;
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

    public UUID getPolicyId() {
        return policyId;
    }

    public UUID getPolicyVersionId() {
        return policyVersionId;
    }

    public UUID getReleaseOperationId() {
        return releaseOperationId;
    }

    public PolicyDecision getDecision() {
        return decision;
    }

    public void setDecision(PolicyDecision decision) {
        this.decision = decision;
    }

    public String getEvaluationHash() {
        return evaluationHash;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
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

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public UUID getEvaluatedBy() {
        return evaluatedBy;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
