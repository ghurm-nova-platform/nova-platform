package ai.nova.platform.knowledge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.knowledge.entity.KnowledgeDocument;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;

public interface KnowledgeDocumentRepository extends JpaRepository<KnowledgeDocument, UUID> {

    Optional<KnowledgeDocument> findByIdAndKnowledgeBaseIdAndProjectIdAndOrganizationId(
            UUID id, UUID knowledgeBaseId, UUID projectId, UUID organizationId);

    boolean existsByKnowledgeBaseIdAndDocumentKey(UUID knowledgeBaseId, String documentKey);

    @Query("""
            SELECT d FROM KnowledgeDocument d
            WHERE d.knowledgeBaseId = :knowledgeBaseId
              AND d.projectId = :projectId
              AND d.organizationId = :organizationId
              AND (:status IS NULL OR d.status = :status)
            """)
    Page<KnowledgeDocument> search(
            @Param("knowledgeBaseId") UUID knowledgeBaseId,
            @Param("projectId") UUID projectId,
            @Param("organizationId") UUID organizationId,
            @Param("status") KnowledgeDocumentStatus status,
            Pageable pageable);

    @Query("""
            SELECT d FROM KnowledgeDocument d
            WHERE d.knowledgeBaseId = :knowledgeBaseId
              AND d.contentHash = :contentHash
              AND d.status <> :archived
            """)
    List<KnowledgeDocument> findNonArchivedByKnowledgeBaseIdAndContentHash(
            @Param("knowledgeBaseId") UUID knowledgeBaseId,
            @Param("contentHash") String contentHash,
            @Param("archived") KnowledgeDocumentStatus archived);

    List<KnowledgeDocument> findByIdInAndProjectIdAndOrganizationId(List<UUID> ids, UUID projectId, UUID organizationId);
}
