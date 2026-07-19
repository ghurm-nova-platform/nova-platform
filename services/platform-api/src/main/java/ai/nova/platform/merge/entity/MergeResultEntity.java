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
@Table(name = "merge_results")
public class MergeResultEntity {

    @Id
    private UUID id;

    @Column(name = "merge_operation_id", nullable = false)
    private UUID mergeOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "merge_method", nullable = false, length = 20)
    private MergeMethod mergeMethod;

    @Column(name = "merged_commit", length = 64)
    private String mergedCommit;

    @Column(name = "pull_request_number", nullable = false)
    private long pullRequestNumber;

    @Column(name = "pull_request_url", length = 2000)
    private String pullRequestUrl;

    @Column(name = "merged_at")
    private Instant mergedAt;

    @Column(name = "merged_by_user_id")
    private UUID mergedByUserId;

    @Column(nullable = false, length = 40)
    private String provider;

    @Column(name = "provider_message", length = 2000)
    private String providerMessage;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MergeResultEntity() {
    }

    public MergeResultEntity(
            UUID id,
            UUID mergeOperationId,
            MergeMethod mergeMethod,
            String mergedCommit,
            long pullRequestNumber,
            String pullRequestUrl,
            Instant mergedAt,
            UUID mergedByUserId,
            String provider,
            String providerMessage,
            Instant createdAt) {
        this.id = id;
        this.mergeOperationId = mergeOperationId;
        this.mergeMethod = mergeMethod;
        this.mergedCommit = mergedCommit;
        this.pullRequestNumber = pullRequestNumber;
        this.pullRequestUrl = pullRequestUrl;
        this.mergedAt = mergedAt;
        this.mergedByUserId = mergedByUserId;
        this.provider = provider;
        this.providerMessage = providerMessage;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMergeOperationId() {
        return mergeOperationId;
    }

    public MergeMethod getMergeMethod() {
        return mergeMethod;
    }

    public String getMergedCommit() {
        return mergedCommit;
    }

    public long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getPullRequestUrl() {
        return pullRequestUrl;
    }

    public Instant getMergedAt() {
        return mergedAt;
    }

    public UUID getMergedByUserId() {
        return mergedByUserId;
    }

    public String getProvider() {
        return provider;
    }

    public String getProviderMessage() {
        return providerMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
