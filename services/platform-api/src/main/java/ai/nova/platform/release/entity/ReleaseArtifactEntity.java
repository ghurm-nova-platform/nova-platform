package ai.nova.platform.release.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "release_artifacts")
public class ReleaseArtifactEntity {

    @Id
    private UUID id;

    @Column(name = "release_operation_id", nullable = false)
    private UUID releaseOperationId;

    @Column(name = "artifact_type", nullable = false, length = 80)
    private String artifactType;

    @Column(name = "artifact_uri", nullable = false, length = 2000)
    private String artifactUri;

    @Column(name = "artifact_hash", length = 64)
    private String artifactHash;

    @Column(name = "label", length = 255)
    private String label;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReleaseArtifactEntity() {
    }

    public ReleaseArtifactEntity(
            UUID id,
            UUID releaseOperationId,
            String artifactType,
            String artifactUri,
            String artifactHash,
            String label,
            Instant createdAt) {
        this.id = id;
        this.releaseOperationId = releaseOperationId;
        this.artifactType = artifactType;
        this.artifactUri = artifactUri;
        this.artifactHash = artifactHash;
        this.label = label;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReleaseOperationId() {
        return releaseOperationId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public String getArtifactUri() {
        return artifactUri;
    }

    public String getArtifactHash() {
        return artifactHash;
    }

    public String getLabel() {
        return label;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
