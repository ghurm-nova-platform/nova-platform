package ai.nova.platform.knowledge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;

public interface KnowledgeBaseRepository extends JpaRepository<KnowledgeBase, UUID> {

    Optional<KnowledgeBase> findByIdAndProjectIdAndOrganizationId(UUID id, UUID projectId, UUID organizationId);

    boolean existsByProjectIdAndKnowledgeKey(UUID projectId, String knowledgeKey);

    @Query("""
            SELECT kb FROM KnowledgeBase kb
            WHERE kb.organizationId = :organizationId
              AND kb.projectId = :projectId
              AND (:status IS NULL OR kb.status = :status)
              AND (:search IS NULL OR LOWER(kb.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(kb.knowledgeKey) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%')))
            """)
    Page<KnowledgeBase> search(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("status") KnowledgeBaseStatus status,
            @Param("search") String search,
            Pageable pageable);

    List<KnowledgeBase> findByIdInAndProjectIdAndOrganizationId(List<UUID> ids, UUID projectId, UUID organizationId);
}
