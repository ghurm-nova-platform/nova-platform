package ai.nova.platform.knowledge.engine.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.knowledge.engine.entity.Category;
import ai.nova.platform.knowledge.engine.entity.DocumentStatus;
import ai.nova.platform.knowledge.engine.entity.KnowledgeDocumentEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.entity.Visibility;

public interface KnowledgeEngineDocumentRepository extends JpaRepository<KnowledgeDocumentEntity, UUID> {

    Optional<KnowledgeDocumentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<KnowledgeDocumentEntity> findByOrganizationIdAndProjectIdAndStatusNotOrderByUpdatedAtDesc(
            UUID organizationId, UUID projectId, DocumentStatus status);

    List<KnowledgeDocumentEntity> findByOrganizationIdAndStatusNotOrderByUpdatedAtDesc(
            UUID organizationId, DocumentStatus status);

    @Query("""
            SELECT DISTINCT d FROM KnowledgeDocumentEntity d
            LEFT JOIN d.tags t
            WHERE d.organizationId = :orgId
              AND d.status <> :excludeStatus
              AND (:projectId IS NULL OR d.projectId = :projectId)
              AND (:category IS NULL OR d.category = :category)
              AND (:knowledgeType IS NULL OR d.knowledgeType = :knowledgeType)
              AND (:authorId IS NULL OR d.authorId = :authorId)
              AND (:tagName IS NULL OR LOWER(t.name) = LOWER(:tagName))
              AND (:fromDate IS NULL OR d.createdAt >= :fromDate)
              AND (:toDate IS NULL OR d.createdAt <= :toDate)
              AND (
                :query IS NULL OR :query = '' OR
                LOWER(d.title) LIKE LOWER(CONCAT('%', :query, '%')) OR
                LOWER(d.summary) LIKE LOWER(CONCAT('%', :query, '%')) OR
                LOWER(d.content) LIKE LOWER(CONCAT('%', :query, '%'))
              )
            ORDER BY d.updatedAt DESC
            """)
    List<KnowledgeDocumentEntity> searchDocuments(
            @Param("orgId") UUID organizationId,
            @Param("excludeStatus") DocumentStatus excludeStatus,
            @Param("projectId") UUID projectId,
            @Param("category") Category category,
            @Param("knowledgeType") KnowledgeType knowledgeType,
            @Param("authorId") UUID authorId,
            @Param("tagName") String tagName,
            @Param("fromDate") Instant fromDate,
            @Param("toDate") Instant toDate,
            @Param("query") String query);

    @Query("""
            SELECT DISTINCT d FROM KnowledgeDocumentEntity d
            LEFT JOIN d.tags t
            WHERE d.organizationId = :orgId
              AND d.status = :status
              AND d.knowledgeType IN :types
              AND (:projectId IS NULL OR d.projectId = :projectId)
            ORDER BY d.updatedAt DESC
            """)
    List<KnowledgeDocumentEntity> findByOrganizationIdAndKnowledgeTypeIn(
            @Param("orgId") UUID organizationId,
            @Param("status") DocumentStatus status,
            @Param("types") List<KnowledgeType> types,
            @Param("projectId") UUID projectId);

    @Query("""
            SELECT DISTINCT d FROM KnowledgeDocumentEntity d
            LEFT JOIN FETCH d.tags
            WHERE d.id IN :ids
            """)
    List<KnowledgeDocumentEntity> findAllWithTagsByIdIn(@Param("ids") List<UUID> ids);

    @Query("""
            SELECT DISTINCT d FROM KnowledgeDocumentEntity d
            JOIN KnowledgeChunkEntity c ON c.documentId = d.id
            WHERE d.organizationId = :orgId
              AND d.status <> :excludeStatus
              AND LOWER(c.content) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY d.updatedAt DESC
            """)
    List<KnowledgeDocumentEntity> searchByChunkContent(
            @Param("orgId") UUID organizationId,
            @Param("excludeStatus") DocumentStatus excludeStatus,
            @Param("query") String query);
}

