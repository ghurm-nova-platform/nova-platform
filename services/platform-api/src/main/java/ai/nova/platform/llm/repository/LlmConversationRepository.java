package ai.nova.platform.llm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.llm.entity.LlmConversationEntity;

public interface LlmConversationRepository extends JpaRepository<LlmConversationEntity, UUID> {

    List<LlmConversationEntity> findByOrganizationIdAndUserIdOrderByUpdatedAtDesc(UUID organizationId, UUID userId);

    Optional<LlmConversationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

}
