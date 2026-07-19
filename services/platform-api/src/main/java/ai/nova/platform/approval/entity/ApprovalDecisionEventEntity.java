package ai.nova.platform.approval.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_decision_events")
public class ApprovalDecisionEventEntity {

    @Id
    private UUID id;

    @Column(name = "approval_decision_id", nullable = false)
    private UUID approvalDecisionId;

    @Column(name = "event_type", nullable = false, length = 60)
    private String eventType;

    @Column(length = 2000)
    private String detail;

    @Column(name = "actor_user_id")
    private UUID actorUserId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApprovalDecisionEventEntity() {
    }

    public ApprovalDecisionEventEntity(
            UUID id,
            UUID approvalDecisionId,
            String eventType,
            String detail,
            UUID actorUserId,
            Instant createdAt) {
        this.id = id;
        this.approvalDecisionId = approvalDecisionId;
        this.eventType = eventType;
        this.detail = detail;
        this.actorUserId = actorUserId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getApprovalDecisionId() {
        return approvalDecisionId;
    }

    public String getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
