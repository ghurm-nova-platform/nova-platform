package ai.nova.platform.llm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import ai.nova.platform.llm.entity.LlmProviderType;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.llm.entity.LlmProviderStatusEntity;

public interface LlmProviderStatusRepository extends JpaRepository<LlmProviderStatusEntity, UUID> {

    List<LlmProviderStatusEntity> findByOrganizationIdOrderByProviderTypeAsc(UUID organizationId);

    Optional<LlmProviderStatusEntity> findByOrganizationIdAndProviderType(
            UUID organizationId, LlmProviderType providerType);

}
