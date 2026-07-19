package ai.nova.platform.rollback.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rollback_targets")
public class RollbackTargetEntity {

    @Id
    private UUID id;

    @Column(name = "rollback_operation_id", nullable = false)
    private UUID rollbackOperationId;

    @Column(name = "target_release_operation_id", nullable = false)
    private UUID targetReleaseOperationId;

    @Column(name = "target_version", nullable = false, length = 64)
    private String targetVersion;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RollbackTargetEntity() {
    }

    public RollbackTargetEntity(
            UUID id,
            UUID rollbackOperationId,
            UUID targetReleaseOperationId,
            String targetVersion,
            int sortOrder,
            Instant createdAt) {
        this.id = id;
        this.rollbackOperationId = rollbackOperationId;
        this.targetReleaseOperationId = targetReleaseOperationId;
        this.targetVersion = targetVersion;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRollbackOperationId() {
        return rollbackOperationId;
    }

    public UUID getTargetReleaseOperationId() {
        return targetReleaseOperationId;
    }

    public String getTargetVersion() {
        return targetVersion;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
