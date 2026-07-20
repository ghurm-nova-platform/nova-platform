package ai.nova.platform.rollback.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rollback_events")
public class RollbackEventEntity {

    @Id
    private UUID id;

    @Column(name = "rollback_operation_id", nullable = false)
    private UUID rollbackOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private RollbackEventType eventType;

    @Column(name = "detail", length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RollbackEventEntity() {
    }

    public RollbackEventEntity(
            UUID id, UUID rollbackOperationId, RollbackEventType eventType, String detail, Instant createdAt) {
        this.id = id;
        this.rollbackOperationId = rollbackOperationId;
        this.eventType = eventType;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRollbackOperationId() {
        return rollbackOperationId;
    }

    public RollbackEventType getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
