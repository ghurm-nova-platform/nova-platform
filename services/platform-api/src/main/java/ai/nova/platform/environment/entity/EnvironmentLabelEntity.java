package ai.nova.platform.environment.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "environment_labels")
public class EnvironmentLabelEntity {

    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "label_key", nullable = false, length = 120)
    private String labelKey;

    @Column(name = "label_value", nullable = false, length = 500)
    private String labelValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EnvironmentLabelEntity() {
    }

    public EnvironmentLabelEntity(UUID id, UUID environmentId, String labelKey, String labelValue, Instant createdAt) {
        this.id = id;
        this.environmentId = environmentId;
        this.labelKey = labelKey;
        this.labelValue = labelValue;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public String getLabelKey() {
        return labelKey;
    }

    public String getLabelValue() {
        return labelValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
