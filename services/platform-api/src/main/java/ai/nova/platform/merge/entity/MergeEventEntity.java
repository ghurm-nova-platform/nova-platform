package ai.nova.platform.merge.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "merge_events")
public class MergeEventEntity {

    @Id
    private UUID id;

    @Column(name = "merge_operation_id", nullable = false)
    private UUID mergeOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private MergeEventType eventType;

    @Column(length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected MergeEventEntity() {
    }

    public MergeEventEntity(UUID id, UUID mergeOperationId, MergeEventType eventType, String detail, Instant createdAt) {
        this.id = id;
        this.mergeOperationId = mergeOperationId;
        this.eventType = eventType;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMergeOperationId() {
        return mergeOperationId;
    }

    public MergeEventType getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
