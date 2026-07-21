package ai.nova.platform.knowledge.engine.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.engine.entity.KnowledgeTagEntity;

public interface KnowledgeEngineTagRepository extends JpaRepository<KnowledgeTagEntity, UUID> {

    Optional<KnowledgeTagEntity> findByOrganizationIdAndNameIgnoreCase(UUID organizationId, String name);

    List<KnowledgeTagEntity> findByOrganizationIdOrderByNameAsc(UUID organizationId);
}

