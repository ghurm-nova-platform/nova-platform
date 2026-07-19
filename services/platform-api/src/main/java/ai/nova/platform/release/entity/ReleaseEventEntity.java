package ai.nova.platform.release.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "release_events")
public class ReleaseEventEntity {

    @Id
    private UUID id;

    @Column(name = "release_operation_id", nullable = false)
    private UUID releaseOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private ReleaseEventType eventType;

    @Column(name = "detail", length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ReleaseEventEntity() {
    }

    public ReleaseEventEntity(
            UUID id, UUID releaseOperationId, ReleaseEventType eventType, String detail, Instant createdAt) {
        this.id = id;
        this.releaseOperationId = releaseOperationId;
        this.eventType = eventType;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getReleaseOperationId() {
        return releaseOperationId;
    }

    public ReleaseEventType getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
