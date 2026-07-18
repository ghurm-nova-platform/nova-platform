package ai.nova.platform.modelcatalog.entity;

import java.io.Serializable;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;

@Entity
@Table(name = "ai_model_capabilities")
public class AiModelCapabilityEntity {

    @EmbeddedId
    private AiModelCapabilityId id;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public AiModelCapabilityEntity() {
    }

    public AiModelCapabilityId getId() {
        return id;
    }

    public void setId(AiModelCapabilityId id) {
        this.id = id;
    }

    public UUID getModelId() {
        return id != null ? id.getModelId() : null;
    }

    public AiModelCapability getCapability() {
        return id != null ? id.getCapability() : null;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    @Embeddable
    public static class AiModelCapabilityId implements Serializable {

        @Column(name = "model_id", nullable = false)
        private UUID modelId;

        @Enumerated(EnumType.STRING)
        @Column(name = "capability", nullable = false, length = 50)
        private AiModelCapability capability;

        public AiModelCapabilityId() {
        }

        public AiModelCapabilityId(UUID modelId, AiModelCapability capability) {
            this.modelId = modelId;
            this.capability = capability;
        }

        public UUID getModelId() {
            return modelId;
        }

        public void setModelId(UUID modelId) {
            this.modelId = modelId;
        }

        public AiModelCapability getCapability() {
            return capability;
        }

        public void setCapability(AiModelCapability capability) {
            this.capability = capability;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof AiModelCapabilityId that)) {
                return false;
            }
            return Objects.equals(modelId, that.modelId) && capability == that.capability;
        }

        @Override
        public int hashCode() {
            return Objects.hash(modelId, capability);
        }
    }
}
