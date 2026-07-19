package ai.nova.platform.merge.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "merge_operations")
public class MergeOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "approval_decision_id", nullable = false)
    private UUID approvalDecisionId;

    @Column(name = "pull_request_operation_id", nullable = false)
    private UUID pullRequestOperationId;

    @Column(name = "git_operation_id", nullable = false)
    private UUID gitOperationId;

    @Column(name = "patch_result_id", nullable = false)
    private UUID patchResultId;

    @Column(name = "ci_observation_operation_id")
    private UUID ciObservationOperationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MergeStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "merge_method", nullable = false, length = 20)
    private MergeMethod mergeMethod;

    @Column(name = "evidence_fingerprint", nullable = false, length = 64)
    private String evidenceFingerprint;

    @Column(name = "decision_fingerprint", nullable = false, length = 64)
    private String decisionFingerprint;

    @Column(name = "expected_patch_hash", nullable = false, length = 64)
    private String expectedPatchHash;

    @Column(name = "expected_commit_hash", nullable = false, length = 64)
    private String expectedCommitHash;

    @Column(name = "expected_pr_head_sha", nullable = false, length = 64)
    private String expectedPrHeadSha;

    @Column(name = "pull_request_number", nullable = false)
    private long pullRequestNumber;

    @Column(name = "repository_owner", nullable = false, length = 255)
    private String repositoryOwner;

    @Column(name = "repository_name", nullable = false, length = 255)
    private String repositoryName;

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

    protected MergeOperationEntity() {
    }

    public MergeOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID taskId,
            UUID approvalDecisionId,
            UUID pullRequestOperationId,
            UUID gitOperationId,
            UUID patchResultId,
            UUID ciObservationOperationId,
            MergeStatus status,
            MergeMethod mergeMethod,
            String evidenceFingerprint,
            String decisionFingerprint,
            String expectedPatchHash,
            String expectedCommitHash,
            String expectedPrHeadSha,
            long pullRequestNumber,
            String repositoryOwner,
            String repositoryName,
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
        this.approvalDecisionId = approvalDecisionId;
        this.pullRequestOperationId = pullRequestOperationId;
        this.gitOperationId = gitOperationId;
        this.patchResultId = patchResultId;
        this.ciObservationOperationId = ciObservationOperationId;
        this.status = status;
        this.mergeMethod = mergeMethod;
        this.evidenceFingerprint = evidenceFingerprint;
        this.decisionFingerprint = decisionFingerprint;
        this.expectedPatchHash = expectedPatchHash;
        this.expectedCommitHash = expectedCommitHash;
        this.expectedPrHeadSha = expectedPrHeadSha;
        this.pullRequestNumber = pullRequestNumber;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
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

    public UUID getApprovalDecisionId() {
        return approvalDecisionId;
    }

    public UUID getPullRequestOperationId() {
        return pullRequestOperationId;
    }

    public UUID getGitOperationId() {
        return gitOperationId;
    }

    public UUID getPatchResultId() {
        return patchResultId;
    }

    public UUID getCiObservationOperationId() {
        return ciObservationOperationId;
    }

    public MergeStatus getStatus() {
        return status;
    }

    public void setStatus(MergeStatus status) {
        this.status = status;
    }

    public MergeMethod getMergeMethod() {
        return mergeMethod;
    }

    public void setMergeMethod(MergeMethod mergeMethod) {
        this.mergeMethod = mergeMethod;
    }

    public String getEvidenceFingerprint() {
        return evidenceFingerprint;
    }

    public String getDecisionFingerprint() {
        return decisionFingerprint;
    }

    public String getExpectedPatchHash() {
        return expectedPatchHash;
    }

    public String getExpectedCommitHash() {
        return expectedCommitHash;
    }

    public String getExpectedPrHeadSha() {
        return expectedPrHeadSha;
    }

    public long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    public String getRepositoryName() {
        return repositoryName;
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

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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
