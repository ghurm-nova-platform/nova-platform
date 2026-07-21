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
@Table(name = "collaboration_messages")
public class CollaborationMessageEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "sender_role", nullable = false, length = 40)
    private CollaborationParticipantRole senderRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private CollaborationMessageType messageType;

    @Column(name = "content", nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CollaborationMessageEntity() {
    }

    public CollaborationMessageEntity(
            UUID id,
            UUID sessionId,
            UUID organizationId,
            CollaborationParticipantRole senderRole,
            CollaborationMessageType messageType,
            String content,
            UUID taskId,
            Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.organizationId = organizationId;
        this.senderRole = senderRole;
        this.messageType = messageType;
        this.content = content;
        this.taskId = taskId;
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

    public CollaborationParticipantRole getSenderRole() {
        return senderRole;
    }

    public CollaborationMessageType getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
