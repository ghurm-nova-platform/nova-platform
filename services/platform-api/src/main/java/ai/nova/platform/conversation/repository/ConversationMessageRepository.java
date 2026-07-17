package ai.nova.platform.conversation.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.conversation.entity.ConversationMessage;

public interface ConversationMessageRepository extends JpaRepository<ConversationMessage, UUID> {

    Page<ConversationMessage> findByConversationIdAndOrganizationIdAndProjectIdOrderBySequenceNumberAsc(
            UUID conversationId, UUID organizationId, UUID projectId, Pageable pageable);

    List<ConversationMessage> findAllByConversationIdAndOrganizationIdAndProjectIdOrderBySequenceNumberAsc(
            UUID conversationId, UUID organizationId, UUID projectId);

    List<ConversationMessage> findByConversationIdAndOrganizationIdAndProjectIdOrderBySequenceNumberDesc(
            UUID conversationId, UUID organizationId, UUID projectId);
}
