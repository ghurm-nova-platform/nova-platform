package ai.nova.platform.ci.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ci_observation_operations")
public class CiObservationOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "pull_request_operation_id", nullable = false)
    private UUID pullRequestOperationId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CiObservationStatus status;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "repository_owner", length = 255)
    private String repositoryOwner;

    @Column(name = "repository_name", length = 255)
    private String repositoryName;

    @Column(name = "source_branch", nullable = false, length = 255)
    private String sourceBranch;

    @Column(name = "target_branch", length = 255)
    private String targetBranch;

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(name = "pull_request_number")
    private Long pullRequestNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "overall_status", nullable = false, length = 20)
    private CiOverallStatus overallStatus;

    @Column(name = "failure_summary", length = 4000)
    private String failureSummary;

    @Column(name = "retry_recommendation", length = 2000)
    private String retryRecommendation;

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

    protected CiObservationOperationEntity() {
    }

    public CiObservationOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID taskId,
            UUID pullRequestOperationId,
            CiObservationStatus status,
            String provider,
            String repositoryOwner,
            String repositoryName,
            String sourceBranch,
            String targetBranch,
            String commitHash,
            Long pullRequestNumber,
            CiOverallStatus overallStatus,
            String failureSummary,
            String retryRecommendation,
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
        this.pullRequestOperationId = pullRequestOperationId;
        this.status = status;
        this.provider = provider;
        this.repositoryOwner = repositoryOwner;
        this.repositoryName = repositoryName;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.commitHash = commitHash;
        this.pullRequestNumber = pullRequestNumber;
        this.overallStatus = overallStatus;
        this.failureSummary = failureSummary;
        this.retryRecommendation = retryRecommendation;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public void updateStatus(CiObservationStatus status) {
        this.status = status;
        this.updatedAt = Instant.now();
    }

    public void markSucceeded(
            CiOverallStatus overallStatus,
            String failureSummary,
            String retryRecommendation,
            Instant completedAt) {
        this.status = CiObservationStatus.SUCCEEDED;
        this.overallStatus = overallStatus;
        this.failureSummary = failureSummary;
        this.retryRecommendation = retryRecommendation;
        this.errorCode = null;
        this.errorMessage = null;
        this.completedAt = completedAt;
        this.updatedAt = completedAt;
    }

    public void markFailed(String errorCode, String errorMessage, Instant completedAt) {
        this.status = CiObservationStatus.FAILED;
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

    public UUID getPullRequestOperationId() {
        return pullRequestOperationId;
    }

    public CiObservationStatus getStatus() {
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

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public CiOverallStatus getOverallStatus() {
        return overallStatus;
    }

    public String getFailureSummary() {
        return failureSummary;
    }

    public String getRetryRecommendation() {
        return retryRecommendation;
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
