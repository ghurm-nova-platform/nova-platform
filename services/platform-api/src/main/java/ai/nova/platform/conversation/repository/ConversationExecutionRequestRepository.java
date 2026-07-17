package ai.nova.platform.conversation.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.conversation.entity.ConversationExecutionRequest;

public interface ConversationExecutionRequestRepository
        extends JpaRepository<ConversationExecutionRequest, UUID> {

    Optional<ConversationExecutionRequest> findByConversationIdAndClientRequestId(
            UUID conversationId, UUID clientRequestId);
}
