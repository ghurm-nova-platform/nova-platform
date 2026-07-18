package ai.nova.platform.git.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "git_operations")
public class GitOperationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "patch_result_id", nullable = false)
    private UUID patchResultId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private GitStatus status;

    @Column(name = "branch_name", nullable = false, length = 255)
    private String branchName;

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(name = "patch_hash", nullable = false, length = 64)
    private String patchHash;

    @Column(name = "repository_path", nullable = false, length = 2000)
    private String repositoryPath;

    @Column(name = "base_ref", nullable = false, length = 255)
    private String baseRef;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "validation_message", length = 2000)
    private String validationMessage;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GitOperationEntity() {
    }

    public GitOperationEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID runId,
            UUID taskId,
            UUID patchResultId,
            GitStatus status,
            String branchName,
            String commitHash,
            String patchHash,
            String repositoryPath,
            String baseRef,
            String errorCode,
            String validationMessage,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.runId = runId;
        this.taskId = taskId;
        this.patchResultId = patchResultId;
        this.status = status;
        this.branchName = branchName;
        this.commitHash = commitHash;
        this.patchHash = patchHash;
        this.repositoryPath = repositoryPath;
        this.baseRef = baseRef;
        this.errorCode = errorCode;
        this.validationMessage = validationMessage;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
    }

    public void markSucceeded(String commitHash, String validationMessage, Instant completedAt) {
        this.status = GitStatus.SUCCEEDED;
        this.commitHash = commitHash;
        this.errorCode = null;
        this.validationMessage = validationMessage;
        this.completedAt = completedAt;
    }

    public void markFailed(String errorCode, String validationMessage, Instant completedAt) {
        this.status = GitStatus.FAILED;
        this.errorCode = errorCode;
        this.validationMessage = validationMessage;
        this.completedAt = completedAt;
        this.commitHash = null;
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

    public UUID getRunId() {
        return runId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getPatchResultId() {
        return patchResultId;
    }

    public GitStatus getStatus() {
        return status;
    }

    public String getBranchName() {
        return branchName;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getPatchHash() {
        return patchHash;
    }

    public String getRepositoryPath() {
        return repositoryPath;
    }

    public String getBaseRef() {
        return baseRef;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getValidationMessage() {
        return validationMessage;
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
}
