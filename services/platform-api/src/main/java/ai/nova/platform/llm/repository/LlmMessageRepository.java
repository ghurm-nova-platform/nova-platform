package ai.nova.platform.llm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.llm.entity.LlmMessageEntity;

public interface LlmMessageRepository extends JpaRepository<LlmMessageEntity, UUID> {

    List<LlmMessageEntity> findByConversationIdOrderBySequenceNoAsc(UUID conversationId);

    Optional<LlmMessageEntity> findTopByConversationIdOrderBySequenceNoDesc(UUID conversationId);

}
