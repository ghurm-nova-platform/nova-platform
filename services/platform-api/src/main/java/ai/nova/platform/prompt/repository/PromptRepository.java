package ai.nova.platform.prompt.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.prompt.entity.Prompt;
import ai.nova.platform.prompt.entity.PromptStatus;
import ai.nova.platform.prompt.entity.PromptType;

public interface PromptRepository extends JpaRepository<Prompt, UUID> {

    boolean existsByProjectIdAndNameIgnoreCase(UUID projectId, String name);

    boolean existsByProjectIdAndNameIgnoreCaseAndIdNot(UUID projectId, String name, UUID id);

    Optional<Prompt> findByIdAndProjectIdAndOrganizationId(UUID id, UUID projectId, UUID organizationId);

    @Query("""
            SELECT p FROM Prompt p
            WHERE p.organizationId = :organizationId
              AND p.projectId = :projectId
              AND (:status IS NULL OR p.status = :status)
              AND (:type IS NULL OR p.promptType = :type)
              AND (
                   :search IS NULL
                   OR LOWER(p.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(COALESCE(p.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              )
              AND (
                   :tag IS NULL
                   OR EXISTS (
                        SELECT 1 FROM PromptTag t
                        WHERE t.promptId = p.id
                          AND LOWER(t.tagName) = LOWER(CAST(:tag AS string))
                   )
              )
            """)
    Page<Prompt> search(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            @Param("status") PromptStatus status,
            @Param("type") PromptType type,
            @Param("tag") String tag,
            Pageable pageable);
}
