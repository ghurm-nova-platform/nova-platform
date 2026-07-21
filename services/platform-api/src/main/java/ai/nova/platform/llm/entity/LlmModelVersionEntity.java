package ai.nova.platform.llm.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "llm_model_versions")
public class LlmModelVersionEntity {

    @Id
    private UUID id;

    @Column(name = "model_id", nullable = false)
    private UUID modelId;

    @Column(name = "version_label", nullable = false, length = 80)
    private String versionLabel;

    @Column(name = "artifact_uri", length = 1000)
    private String artifactUri;

    private String checksum;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Column(name = "is_current", nullable = false)
    private boolean current;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected LlmModelVersionEntity() {
    }

    public LlmModelVersionEntity(
            UUID id, UUID modelId, String versionLabel, String artifactUri, boolean current, Instant createdAt) {
        this.id = id;
        this.modelId = modelId;
        this.versionLabel = versionLabel;
        this.artifactUri = artifactUri;
        this.current = current;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getModelId() { return modelId; }
    public String getVersionLabel() { return versionLabel; }
    public String getArtifactUri() { return artifactUri; }
    public String getChecksum() { return checksum; }
    public Long getSizeBytes() { return sizeBytes; }
    public boolean isCurrent() { return current; }
    public Instant getCreatedAt() { return createdAt; }

    public void setChecksum(String checksum) { this.checksum = checksum; }
    public void setSizeBytes(Long sizeBytes) { this.sizeBytes = sizeBytes; }
    public void setCurrent(boolean current) { this.current = current; }
    public void setArtifactUri(String artifactUri) { this.artifactUri = artifactUri; }
}
