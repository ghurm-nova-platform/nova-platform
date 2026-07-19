package ai.nova.platform.release.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "release_contents")
public class ReleaseContentEntity {

    @Id
    private UUID id;

    @Column(name = "release_operation_id", nullable = false)
    private UUID releaseOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 40)
    private ReleaseContentType contentType;

    @Column(name = "reference_id")
    private UUID referenceId;

    @Column(name = "commit_sha", length = 64)
    private String commitSha;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReleaseContentEntity() {
    }

    public ReleaseContentEntity(
            UUID id,
            UUID releaseOperationId,
            ReleaseContentType contentType,
            UUID referenceId,
            String commitSha,
            int sortOrder,
            Instant createdAt) {
        this.id = id;
        this.releaseOperationId = releaseOperationId;
        this.contentType = contentType;
        this.referenceId = referenceId;
        this.commitSha = commitSha;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReleaseOperationId() {
        return releaseOperationId;
    }

    public ReleaseContentType getContentType() {
        return contentType;
    }

    public UUID getReferenceId() {
        return referenceId;
    }

    public String getCommitSha() {
        return commitSha;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
