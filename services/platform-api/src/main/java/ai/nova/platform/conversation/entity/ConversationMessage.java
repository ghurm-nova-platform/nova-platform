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
@Table(name = "conversation_messages")
public class ConversationMessage {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ConversationMessageRole role;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Column(name = "token_count")
    private Integer tokenCount;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "client_request_id")
    private UUID clientRequestId;

    protected ConversationMessage() {
    }

    public ConversationMessage(
            UUID id,
            UUID conversationId,
            UUID organizationId,
            UUID projectId,
            UUID agentId,
            UUID executionId,
            ConversationMessageRole role,
            String content,
            int sequenceNumber,
            UUID createdBy,
            Instant createdAt,
            UUID clientRequestId) {
        this.id = id;
        this.conversationId = conversationId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.agentId = agentId;
        this.executionId = executionId;
        this.role = role;
        this.content = content;
        this.sequenceNumber = sequenceNumber;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.clientRequestId = clientRequestId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public ConversationMessageRole getRole() {
        return role;
    }

    public String getContent() {
        return content;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public Integer getTokenCount() {
        return tokenCount;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getClientRequestId() {
        return clientRequestId;
    }
}
