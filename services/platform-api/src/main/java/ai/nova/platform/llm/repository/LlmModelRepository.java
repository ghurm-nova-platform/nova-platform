package ai.nova.platform.llm.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.llm.entity.LlmModelEntity;

public interface LlmModelRepository extends JpaRepository<LlmModelEntity, UUID> {

    List<LlmModelEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    Optional<LlmModelEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<LlmModelEntity> findByOrganizationIdAndCode(UUID organizationId, String code);

    boolean existsByOrganizationIdAndCode(UUID organizationId, String code);

}
