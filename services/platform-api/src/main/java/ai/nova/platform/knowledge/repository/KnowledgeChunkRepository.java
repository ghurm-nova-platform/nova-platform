package ai.nova.platform.knowledge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.knowledge.entity.KnowledgeChunk;

public interface KnowledgeChunkRepository extends JpaRepository<KnowledgeChunk, UUID> {

    List<KnowledgeChunk> findByDocumentIdAndProjectIdAndOrganizationIdOrderByChunkIndexAsc(
            UUID documentId, UUID projectId, UUID organizationId);

    void deleteByDocumentIdAndProjectIdAndOrganizationId(UUID documentId, UUID projectId, UUID organizationId);

    List<KnowledgeChunk> findByIdInAndProjectIdAndOrganizationId(List<UUID> ids, UUID projectId, UUID organizationId);
}
