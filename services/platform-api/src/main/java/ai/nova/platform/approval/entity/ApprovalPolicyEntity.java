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
@Table(name = "approval_policies")
public class ApprovalPolicyEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(nullable = false, length = 200)
    private String name;

    @Column(length = 2000)
    private String description;

    @Column(nullable = false)
    private int version;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ApprovalPolicyStatus status;

    @Column(name = "is_default", nullable = false)
    private boolean isDefault;

    @Column(name = "required_human_approvals", nullable = false)
    private int requiredHumanApprovals;

    @Column(name = "require_distinct_approvers", nullable = false)
    private boolean requireDistinctApprovers;

    @Column(name = "prohibit_author_approval", nullable = false)
    private boolean prohibitAuthorApproval;

    @Column(name = "require_ci_success", nullable = false)
    private boolean requireCiSuccess;

    @Column(name = "require_review_approved", nullable = false)
    private boolean requireReviewApproved;

    @Column(name = "minimum_review_score")
    private Integer minimumReviewScore;

    @Column(name = "require_testing_success", nullable = false)
    private boolean requireTestingSuccess;

    @Column(name = "minimum_estimated_coverage")
    private Integer minimumEstimatedCoverage;

    @Column(name = "require_no_critical_findings", nullable = false)
    private boolean requireNoCriticalFindings;

    @Column(name = "require_no_high_findings", nullable = false)
    private boolean requireNoHighFindings;

    @Column(name = "require_repair_success_when_failed", nullable = false)
    private boolean requireRepairSuccessWhenFailed;

    @Column(name = "require_pull_request_open", nullable = false)
    private boolean requirePullRequestOpen;

    @Column(name = "require_exact_commit_match", nullable = false)
    private boolean requireExactCommitMatch;

    @Column(name = "decision_validity_minutes")
    private Integer decisionValidityMinutes;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_by")
    private UUID updatedBy;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApprovalPolicyEntity() {
    }

    public ApprovalPolicyEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String name,
            String description,
            int version,
            ApprovalPolicyStatus status,
            boolean isDefault,
            int requiredHumanApprovals,
            boolean requireDistinctApprovers,
            boolean prohibitAuthorApproval,
            boolean requireCiSuccess,
            boolean requireReviewApproved,
            Integer minimumReviewScore,
            boolean requireTestingSuccess,
            Integer minimumEstimatedCoverage,
            boolean requireNoCriticalFindings,
            boolean requireNoHighFindings,
            boolean requireRepairSuccessWhenFailed,
            boolean requirePullRequestOpen,
            boolean requireExactCommitMatch,
            Integer decisionValidityMinutes,
            UUID createdBy,
            Instant createdAt,
            UUID updatedBy,
            Instant updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.version = version;
        this.status = status;
        this.isDefault = isDefault;
        this.requiredHumanApprovals = requiredHumanApprovals;
        this.requireDistinctApprovers = requireDistinctApprovers;
        this.prohibitAuthorApproval = prohibitAuthorApproval;
        this.requireCiSuccess = requireCiSuccess;
        this.requireReviewApproved = requireReviewApproved;
        this.minimumReviewScore = minimumReviewScore;
        this.requireTestingSuccess = requireTestingSuccess;
        this.minimumEstimatedCoverage = minimumEstimatedCoverage;
        this.requireNoCriticalFindings = requireNoCriticalFindings;
        this.requireNoHighFindings = requireNoHighFindings;
        this.requireRepairSuccessWhenFailed = requireRepairSuccessWhenFailed;
        this.requirePullRequestOpen = requirePullRequestOpen;
        this.requireExactCommitMatch = requireExactCommitMatch;
        this.decisionValidityMinutes = decisionValidityMinutes;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedBy = updatedBy;
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

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public int getVersion() {
        return version;
    }

    public ApprovalPolicyStatus getStatus() {
        return status;
    }

    public void setStatus(ApprovalPolicyStatus status) {
        this.status = status;
    }

    public boolean isDefault() {
        return isDefault;
    }

    public int getRequiredHumanApprovals() {
        return requiredHumanApprovals;
    }

    public void setRequiredHumanApprovals(int requiredHumanApprovals) {
        this.requiredHumanApprovals = requiredHumanApprovals;
    }

    public boolean isRequireDistinctApprovers() {
        return requireDistinctApprovers;
    }

    public boolean isProhibitAuthorApproval() {
        return prohibitAuthorApproval;
    }

    public void setProhibitAuthorApproval(boolean prohibitAuthorApproval) {
        this.prohibitAuthorApproval = prohibitAuthorApproval;
    }

    public boolean isRequireCiSuccess() {
        return requireCiSuccess;
    }

    public boolean isRequireReviewApproved() {
        return requireReviewApproved;
    }

    public Integer getMinimumReviewScore() {
        return minimumReviewScore;
    }

    public boolean isRequireTestingSuccess() {
        return requireTestingSuccess;
    }

    public Integer getMinimumEstimatedCoverage() {
        return minimumEstimatedCoverage;
    }

    public boolean isRequireNoCriticalFindings() {
        return requireNoCriticalFindings;
    }

    public boolean isRequireNoHighFindings() {
        return requireNoHighFindings;
    }

    public boolean isRequireRepairSuccessWhenFailed() {
        return requireRepairSuccessWhenFailed;
    }

    public boolean isRequirePullRequestOpen() {
        return requirePullRequestOpen;
    }

    public boolean isRequireExactCommitMatch() {
        return requireExactCommitMatch;
    }

    public Integer getDecisionValidityMinutes() {
        return decisionValidityMinutes;
    }

    public void setDecisionValidityMinutes(Integer decisionValidityMinutes) {
        this.decisionValidityMinutes = decisionValidityMinutes;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
