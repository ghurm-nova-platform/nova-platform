package ai.nova.platform.pullrequest.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pull_request_operations")
public class PullRequestOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "git_operation_id", nullable = false)
    private UUID gitOperationId;

    @Column(name = "patch_result_id")
    private UUID patchResultId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PullRequestStatus status;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "repository_owner", length = 255)
    private String repositoryOwner;

    @Column(name = "repository_name", length = 255)
    private String repositoryName;

    @Column(name = "remote_name", length = 100)
    private String remoteName;

    @Column(name = "remote_url", length = 2000)
    private String remoteUrl;

    @Column(name = "source_branch", nullable = false, length = 255)
    private String sourceBranch;

    @Column(name = "target_branch", nullable = false, length = 255)
    private String targetBranch;

    @Column(name = "local_commit_hash", nullable = false, length = 64)
    private String localCommitHash;

    @Column(name = "remote_commit_hash", length = 64)
    private String remoteCommitHash;

    @Column(name = "patch_hash", nullable = false, length = 64)
    private String patchHash;

    @Column(name = "pull_request_number")
    private Long pullRequestNumber;

    @Column(name = "pull_request_url", length = 2000)
    private String pullRequestUrl;

    @Column(name = "pull_request_title", length = 500)
    private String pullRequestTitle;

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

    protected PullRequestOperationEntity() {
    }

    public PullRequestOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID taskId,
            UUID gitOperationId,
            UUID patchResultId,
            PullRequestStatus status,
            String provider,
            String repositoryOwner,
            String repositoryName,
            String remoteName,
            String remoteUrl,
            String sourceBranch,
            String targetBranch,
            String localCommitHash,
            String remoteCommitHash,
            String patchHash,
            Long pullRequestNumber,
            String pullRequestUrl,
            String pullRequestTitle,
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
        this.gitOperationId = gitOperationId;
        this.patchResultId = patchResultId;
        this.status = status;
        this.provider = provider;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.remoteName = remoteName;
        this.remoteUrl = remoteUrl;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.localCommitHash = localCommitHash;
        this.remoteCommitHash = remoteCommitHash;
        this.patchHash = patchHash;
        this.pullRequestNumber = pullRequestNumber;
        this.pullRequestUrl = pullRequestUrl;
        this.pullRequestTitle = pullRequestTitle;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateStatus(PullRequestStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void markPushed(String remoteCommitHash) {
        this.status = PullRequestStatus.PUSHED;
        this.remoteCommitHash = remoteCommitHash;
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(
            Long pullRequestNumber,
            String pullRequestUrl,
            String pullRequestTitle,
            String remoteCommitHash,
            Instant completedAt) {
        this.status = PullRequestStatus.SUCCEEDED;
        this.pullRequestNumber = pullRequestNumber;
        this.pullRequestUrl = pullRequestUrl;
        this.pullRequestTitle = pullRequestTitle;
        this.remoteCommitHash = remoteCommitHash;
        this.errorCode = null;
        this.errorMessage = null;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }

    public void markFailed(String errorCode, String errorMessage, Instant completedAt) {
        this.status = PullRequestStatus.FAILED;
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

    public UUID getGitOperationId() {
        return gitOperationId;
    }

    public UUID getPatchResultId() {
        return patchResultId;
    }

    public PullRequestStatus getStatus() {
        return status;
    }

    public String getProvider() {
        return provider;
    }

    public String getRepositoryOwner() {
        return repositoryOwner;
    }

    public String getRepositoryName() {
        return repositoryName;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public String getRemoteUrl() {
        return remoteUrl;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getLocalCommitHash() {
        return localCommitHash;
    }

    public String getRemoteCommitHash() {
        return remoteCommitHash;
    }

    public String getPatchHash() {
        return patchHash;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public String getPullRequestTitle() {
        return pullRequestTitle;
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
