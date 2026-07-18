package ai.nova.platform.knowledge.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.knowledge.entity.KnowledgeEmbedding;

public interface KnowledgeEmbeddingRepository extends JpaRepository<KnowledgeEmbedding, UUID> {

    void deleteByDocumentIdAndProjectIdAndOrganizationId(UUID documentId, UUID projectId, UUID organizationId);

    @Query("""
            SELECT e FROM KnowledgeEmbedding e
            WHERE e.organizationId = :organizationId
              AND e.projectId = :projectId
              AND e.knowledgeBaseId = :knowledgeBaseId
              AND e.providerKey = :providerKey
              AND e.model = :model
            """)
    List<KnowledgeEmbedding> findCandidates(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("knowledgeBaseId") UUID knowledgeBaseId,
            @Param("providerKey") String providerKey,
            @Param("model") String model);
}
