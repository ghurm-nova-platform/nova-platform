package ai.nova.platform.pullrequest.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "pull_request_records")
public class PullRequestRecordEntity {

    @Id
    private UUID id;

    @Column(name = "pull_request_operation_id", nullable = false)
    private UUID pullRequestOperationId;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "external_id", length = 255)
    private String externalId;

    @Column(name = "pull_request_number", nullable = false)
    private Long pullRequestNumber;

    @Column(name = "pull_request_url", nullable = false, length = 2000)
    private String pullRequestUrl;

    @Column(nullable = false, length = 500)
    private String title;

    @Column(name = "source_branch", nullable = false, length = 255)
    private String sourceBranch;

    @Column(name = "target_branch", nullable = false, length = 255)
    private String targetBranch;

    @Column(nullable = false, length = 40)
    private String state;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PullRequestRecordEntity() {
    }

    public PullRequestRecordEntity(
            UUID id,
            UUID pullRequestOperationId,
            String provider,
            String externalId,
            Long pullRequestNumber,
            String pullRequestUrl,
            String title,
            String sourceBranch,
            String targetBranch,
            String state,
            Instant createdAt) {
        this.id = id;
        this.pullRequestOperationId = pullRequestOperationId;
        this.provider = provider;
        this.externalId = externalId;
        this.pullRequestNumber = pullRequestNumber;
        this.pullRequestUrl = pullRequestUrl;
        this.title = title;
        this.sourceBranch = sourceBranch;
        this.targetBranch = targetBranch;
        this.state = state;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPullRequestOperationId() {
        return pullRequestOperationId;
    }

    public String getProvider() {
        return provider;
    }

    public String getExternalId() {
        return externalId;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getSourceBranch() {
        return sourceBranch;
    }

    public String getTargetBranch() {
        return targetBranch;
    }

    public String getState() {
        return state;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
