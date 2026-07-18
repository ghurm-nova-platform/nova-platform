package ai.nova.platform.git.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "git_commits")
public class GitCommitEntity {

    @Id
    private UUID id;

    @Column(name = "git_operation_id", nullable = false)
    private UUID gitOperationId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "commit_hash", nullable = false, length = 64)
    private String commitHash;

    @Column(nullable = false, length = 1000)
    private String message;

    @Column(name = "author_name", nullable = false, length = 255)
    private String authorName;

    @Column(name = "author_email", nullable = false, length = 255)
    private String authorEmail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected GitCommitEntity() {
    }

    public GitCommitEntity(
            UUID id,
            UUID gitOperationId,
            UUID organizationId,
            String commitHash,
            String message,
            String authorName,
            String authorEmail,
            Instant createdAt) {
        this.id = id;
        this.gitOperationId = gitOperationId;
        this.organizationId = organizationId;
        this.commitHash = commitHash;
        this.message = message;
        this.authorName = authorName;
        this.authorEmail = authorEmail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getGitOperationId() {
        return gitOperationId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getMessage() {
        return message;
    }

    public String getAuthorName() {
        return authorName;
    }

    public String getAuthorEmail() {
        return authorEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
