package ai.nova.platform.conversation.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_audit_log")
public class ConversationAuditLog {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ConversationAuditAction action;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    protected ConversationAuditLog() {
    }

    public ConversationAuditLog(
            UUID id,
            UUID conversationId,
            UUID organizationId,
            UUID projectId,
            ConversationAuditAction action,
            String metadata,
            UUID performedBy,
            Instant performedAt,
            String correlationId) {
        this.id = id;
        this.conversationId = conversationId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.action = action;
        this.metadata = metadata;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.correlationId = correlationId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public ConversationAuditAction getAction() {
        return action;
    }

    public String getMetadata() {
        return metadata;
    }
}
