package ai.nova.platform.llm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.llm.entity.LlmPromptTemplateEntity;

public interface LlmPromptTemplateRepository extends JpaRepository<LlmPromptTemplateEntity, UUID> {

    List<LlmPromptTemplateEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<LlmPromptTemplateEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<LlmPromptTemplateEntity> findByOrganizationIdAndCode(UUID organizationId, String code);

}
