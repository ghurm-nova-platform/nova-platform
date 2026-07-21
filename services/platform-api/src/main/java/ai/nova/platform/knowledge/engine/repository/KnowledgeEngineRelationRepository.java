package ai.nova.platform.knowledge.engine.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.engine.entity.KnowledgeRelationEntity;

public interface KnowledgeEngineRelationRepository extends JpaRepository<KnowledgeRelationEntity, UUID> {

    List<KnowledgeRelationEntity> findBySourceDocumentIdOrderByCreatedAtAsc(UUID sourceDocumentId);

    List<KnowledgeRelationEntity> findByOrganizationIdAndSourceDocumentId(
            UUID organizationId, UUID sourceDocumentId);
}

