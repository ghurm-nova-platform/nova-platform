package ai.nova.platform.patch.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patch_artifacts")
public class PatchArtifactEntity {

    @Id
    private UUID id;

    @Column(name = "patch_result_id", nullable = false)
    private UUID patchResultId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "artifact_id", nullable = false)
    private UUID artifactId;

    @Column(nullable = false, length = 1000)
    private String path;

    @Column(nullable = false, length = 255)
    private String filename;

    @Column(nullable = false, length = 50)
    private String language;

    @Column(nullable = false, length = 64)
    private String sha256;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PatchArtifactEntity() {
    }

    public PatchArtifactEntity(
            UUID id,
            UUID patchResultId,
            UUID organizationId,
            UUID artifactId,
            String path,
            String filename,
            String language,
            String sha256,
            Instant createdAt) {
        this.id = id;
        this.patchResultId = patchResultId;
        this.organizationId = organizationId;
        this.artifactId = artifactId;
        this.path = path;
        this.filename = filename;
        this.language = language;
        this.sha256 = sha256;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPatchResultId() {
        return patchResultId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getArtifactId() {
        return artifactId;
    }

    public String getPath() {
        return path;
    }

    public String getFilename() {
        return filename;
    }

    public String getLanguage() {
        return language;
    }

    public String getSha256() {
        return sha256;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
