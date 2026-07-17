package ai.nova.platform.conversation.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "conversation_execution_requests")
public class ConversationExecutionRequest {

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

    @Column(name = "client_request_id", nullable = false)
    private UUID clientRequestId;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "user_message_id")
    private UUID userMessageId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ConversationExecutionRequest() {
    }

    public ConversationExecutionRequest(
            UUID id,
            UUID conversationId,
            UUID organizationId,
            UUID projectId,
            UUID agentId,
            UUID clientRequestId,
            UUID executionId,
            UUID userMessageId,
            Instant createdAt) {
        this.id = id;
        this.conversationId = conversationId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.agentId = agentId;
        this.clientRequestId = clientRequestId;
        this.executionId = executionId;
        this.userMessageId = userMessageId;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getClientRequestId() {
        return clientRequestId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getUserMessageId() {
        return userMessageId;
    }
}
