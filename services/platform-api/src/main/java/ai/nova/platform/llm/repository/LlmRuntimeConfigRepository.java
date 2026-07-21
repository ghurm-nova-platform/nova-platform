package ai.nova.platform.llm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.llm.entity.LlmRuntimeConfigEntity;

public interface LlmRuntimeConfigRepository extends JpaRepository<LlmRuntimeConfigEntity, UUID> {

    List<LlmRuntimeConfigEntity> findByOrganizationIdOrderByConfigKeyAsc(UUID organizationId);

    Optional<LlmRuntimeConfigEntity> findByOrganizationIdAndConfigKey(UUID organizationId, String configKey);

}
