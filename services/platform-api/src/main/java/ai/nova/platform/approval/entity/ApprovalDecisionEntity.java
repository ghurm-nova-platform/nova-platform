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
@Table(name = "approval_decisions")
public class ApprovalDecisionEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "approval_gate_operation_id", nullable = false)
    private UUID approvalGateOperationId;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "policy_version", nullable = false)
    private int policyVersion;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApprovalDecisionValue decision;

    @Column(name = "eligible_for_merge", nullable = false)
    private boolean eligibleForMerge;

    @Column(name = "required_human_approvals", nullable = false)
    private int requiredHumanApprovals;

    @Column(name = "received_human_approvals", nullable = false)
    private int receivedHumanApprovals;

    @Column(name = "rejection_count", nullable = false)
    private int rejectionCount;

    @Column(name = "evidence_fingerprint", nullable = false, length = 64)
    private String evidenceFingerprint;

    @Column(name = "decision_fingerprint", nullable = false, length = 64)
    private String decisionFingerprint;

    @Column(name = "patch_result_id", nullable = false)
    private UUID patchResultId;

    @Column(name = "patch_hash", nullable = false, length = 64)
    private String patchHash;

    @Column(name = "git_operation_id", nullable = false)
    private UUID gitOperationId;

    @Column(name = "commit_hash", nullable = false, length = 64)
    private String commitHash;

    @Column(name = "pull_request_operation_id", nullable = false)
    private UUID pullRequestOperationId;

    @Column(name = "pull_request_number", nullable = false)
    private long pullRequestNumber;

    @Column(name = "pull_request_url", length = 2000)
    private String pullRequestUrl;

    @Column(name = "ci_observation_operation_id")
    private UUID ciObservationOperationId;

    @Column(name = "ci_overall_status", length = 40)
    private String ciOverallStatus;

    @Column(name = "repair_operation_id")
    private UUID repairOperationId;

    @Column(name = "reason_summary", length = 4000)
    private String reasonSummary;

    @Column(name = "valid_until")
    private Instant validUntil;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "rejected_at")
    private Instant rejectedAt;

    @Column(name = "superseded_at")
    private Instant supersededAt;

    @Column(name = "invalidated_at")
    private Instant invalidatedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ApprovalDecisionEntity() {
    }

    public ApprovalDecisionEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID taskId,
            UUID approvalGateOperationId,
            UUID policyId,
            int policyVersion,
            ApprovalDecisionValue decision,
            boolean eligibleForMerge,
            int requiredHumanApprovals,
            int receivedHumanApprovals,
            int rejectionCount,
            String evidenceFingerprint,
            String decisionFingerprint,
            UUID patchResultId,
            String patchHash,
            UUID gitOperationId,
            String commitHash,
            UUID pullRequestOperationId,
            long pullRequestNumber,
            String pullRequestUrl,
            UUID ciObservationOperationId,
            String ciOverallStatus,
            UUID repairOperationId,
            String reasonSummary,
            Instant validUntil,
            Instant approvedAt,
            Instant rejectedAt,
            Instant supersededAt,
            Instant invalidatedAt,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.taskId = taskId;
        this.approvalGateOperationId = approvalGateOperationId;
        this.policyId = policyId;
        this.policyVersion = policyVersion;
        this.decision = decision;
        this.eligibleForMerge = eligibleForMerge;
        this.requiredHumanApprovals = requiredHumanApprovals;
        this.receivedHumanApprovals = receivedHumanApprovals;
        this.rejectionCount = rejectionCount;
        this.evidenceFingerprint = evidenceFingerprint;
        this.decisionFingerprint = decisionFingerprint;
        this.patchResultId = patchResultId;
        this.patchHash = patchHash;
        this.gitOperationId = gitOperationId;
        this.commitHash = commitHash;
        this.pullRequestOperationId = pullRequestOperationId;
        this.pullRequestNumber = pullRequestNumber;
        this.pullRequestUrl = pullRequestUrl;
        this.ciObservationOperationId = ciObservationOperationId;
        this.ciOverallStatus = ciOverallStatus;
        this.repairOperationId = repairOperationId;
        this.reasonSummary = reasonSummary;
        this.validUntil = validUntil;
        this.approvedAt = approvedAt;
        this.rejectedAt = rejectedAt;
        this.supersededAt = supersededAt;
        this.invalidatedAt = invalidatedAt;
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

    public UUID getApprovalGateOperationId() {
        return approvalGateOperationId;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public int getPolicyVersion() {
        return policyVersion;
    }

    public ApprovalDecisionValue getDecision() {
        return decision;
    }

    public void setDecision(ApprovalDecisionValue decision) {
        this.decision = decision;
    }

    public boolean isEligibleForMerge() {
        return eligibleForMerge;
    }

    public void setEligibleForMerge(boolean eligibleForMerge) {
        this.eligibleForMerge = eligibleForMerge;
    }

    public int getRequiredHumanApprovals() {
        return requiredHumanApprovals;
    }

    public int getReceivedHumanApprovals() {
        return receivedHumanApprovals;
    }

    public int getRejectionCount() {
        return rejectionCount;
    }

    public String getEvidenceFingerprint() {
        return evidenceFingerprint;
    }

    public String getDecisionFingerprint() {
        return decisionFingerprint;
    }

    public UUID getPatchResultId() {
        return patchResultId;
    }

    public String getPatchHash() {
        return patchHash;
    }

    public UUID getGitOperationId() {
        return gitOperationId;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public UUID getPullRequestOperationId() {
        return pullRequestOperationId;
    }

    public long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public UUID getCiObservationOperationId() {
        return ciObservationOperationId;
    }

    public String getCiOverallStatus() {
        return ciOverallStatus;
    }

    public UUID getRepairOperationId() {
        return repairOperationId;
    }

    public String getReasonSummary() {
        return reasonSummary;
    }

    public void setReasonSummary(String reasonSummary) {
        this.reasonSummary = reasonSummary;
    }

    public Instant getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(Instant validUntil) {
        this.validUntil = validUntil;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public Instant getRejectedAt() {
        return rejectedAt;
    }

    public void setRejectedAt(Instant rejectedAt) {
        this.rejectedAt = rejectedAt;
    }

    public Instant getSupersededAt() {
        return supersededAt;
    }

    public void setSupersededAt(Instant supersededAt) {
        this.supersededAt = supersededAt;
    }

    public Instant getInvalidatedAt() {
        return invalidatedAt;
    }

    public void setInvalidatedAt(Instant invalidatedAt) {
        this.invalidatedAt = invalidatedAt;
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
