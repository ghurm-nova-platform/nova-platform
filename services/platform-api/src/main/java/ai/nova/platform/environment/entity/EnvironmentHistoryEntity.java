package ai.nova.platform.environment.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "environment_history")
public class EnvironmentHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "change_type", nullable = false, length = 60)
    private String changeType;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected EnvironmentHistoryEntity() {
    }

    public EnvironmentHistoryEntity(
            UUID id, UUID environmentId, String changeType, String snapshotJson, UUID createdBy, Instant createdAt) {
        this.id = id;
        this.environmentId = environmentId;
        this.changeType = changeType;
        this.snapshotJson = snapshotJson;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public String getChangeType() {
        return changeType;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
