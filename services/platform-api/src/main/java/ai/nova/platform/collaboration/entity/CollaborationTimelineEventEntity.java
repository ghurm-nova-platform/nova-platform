package ai.nova.platform.collaboration.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "collaboration_timeline_events")
public class CollaborationTimelineEventEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 40)
    private CollaborationTimelineEventType eventType;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(name = "actor_role", length = 40)
    private CollaborationParticipantRole actorRole;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "message_id")
    private UUID messageId;

    @Column(name = "decision_id")
    private UUID decisionId;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CollaborationTimelineEventEntity() {
    }

    public CollaborationTimelineEventEntity(
            UUID id,
            UUID sessionId,
            UUID organizationId,
            CollaborationTimelineEventType eventType,
            String summary,
            CollaborationParticipantRole actorRole,
            UUID taskId,
            UUID messageId,
            UUID decisionId,
            String detailsJson,
            Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.organizationId = organizationId;
        this.eventType = eventType;
        this.summary = summary;
        this.actorRole = actorRole;
        this.taskId = taskId;
        this.messageId = messageId;
        this.decisionId = decisionId;
        this.detailsJson = detailsJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public CollaborationTimelineEventType getEventType() {
        return eventType;
    }

    public String getSummary() {
        return summary;
    }

    public CollaborationParticipantRole getActorRole() {
        return actorRole;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getMessageId() {
        return messageId;
    }

    public UUID getDecisionId() {
        return decisionId;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
