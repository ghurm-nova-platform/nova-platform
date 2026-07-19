package ai.nova.platform.repair.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "repair_inputs")
public class RepairInputEntity {

    @Id
    private UUID id;

    @Column(name = "repair_operation_id", nullable = false)
    private UUID repairOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type", nullable = false, length = 40)
    private RepairInputSource sourceType;

    @Column(name = "source_ref", length = 255)
    private String sourceRef;

    @Column(nullable = false)
    private int priority;

    @Column(nullable = false, length = 4000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RepairInputEntity() {
    }

    public RepairInputEntity(
            UUID id,
            UUID repairOperationId,
            RepairInputSource sourceType,
            String sourceRef,
            int priority,
            String detail,
            Instant createdAt) {
        this.id = id;
        this.repairOperationId = repairOperationId;
        this.sourceType = sourceType;
        this.sourceRef = sourceRef;
        this.priority = priority;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRepairOperationId() {
        return repairOperationId;
    }

    public RepairInputSource getSourceType() {
        return sourceType;
    }

    public String getSourceRef() {
        return sourceRef;
    }

    public int getPriority() {
        return priority;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
