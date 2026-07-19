package ai.nova.platform.repair.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "repair_actions")
public class RepairActionEntity {

    @Id
    private UUID id;

    @Column(name = "repair_operation_id", nullable = false)
    private UUID repairOperationId;

    @Column(name = "action_type", nullable = false, length = 40)
    private String actionType;

    @Column(name = "target_path", length = 1000)
    private String targetPath;

    @Column(nullable = false, length = 2000)
    private String description;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RepairActionEntity() {
    }

    public RepairActionEntity(
            UUID id,
            UUID repairOperationId,
            String actionType,
            String targetPath,
            String description,
            Instant createdAt) {
        this.id = id;
        this.repairOperationId = repairOperationId;
        this.actionType = actionType;
        this.targetPath = targetPath;
        this.description = description;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRepairOperationId() {
        return repairOperationId;
    }

    public String getActionType() {
        return actionType;
    }

    public String getTargetPath() {
        return targetPath;
    }

    public String getDescription() {
        return description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
