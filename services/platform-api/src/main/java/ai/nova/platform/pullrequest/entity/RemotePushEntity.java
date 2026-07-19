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
@Table(name = "remote_pushes")
public class RemotePushEntity {

    @Id
    private UUID id;

    @Column(name = "pull_request_operation_id", nullable = false)
    private UUID pullRequestOperationId;

    @Column(name = "remote_name", nullable = false, length = 100)
    private String remoteName;

    @Column(name = "source_branch", nullable = false, length = 255)
    private String sourceBranch;

    @Column(name = "local_commit_hash", nullable = false, length = 64)
    private String localCommitHash;

    @Column(name = "remote_commit_hash", length = 64)
    private String remoteCommitHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private RemotePushStatus status;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "error_code", length = 80)
    private String errorCode;

    @Column(name = "error_message", length = 2000)
    private String errorMessage;

    protected RemotePushEntity() {
    }

    public RemotePushEntity(
            UUID id,
            UUID pullRequestOperationId,
            String remoteName,
            String sourceBranch,
            String localCommitHash,
            String remoteCommitHash,
            RemotePushStatus status,
            Instant startedAt,
            Instant completedAt,
            String errorCode,
            String errorMessage) {
        this.id = id;
        this.pullRequestOperationId = pullRequestOperationId;
        this.remoteName = remoteName;
        this.sourceBranch = sourceBranch;
        this.localCommitHash = localCommitHash;
        this.remoteCommitHash = remoteCommitHash;
        this.status = status;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPullRequestOperationId() {
        return pullRequestOperationId;
    }

    public String getRemoteName() {
        return remoteName;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getLocalCommitHash() {
        return localCommitHash;
    }

    public String getRemoteCommitHash() {
        return remoteCommitHash;
    }

    public RemotePushStatus getStatus() {
        return status;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }
}
