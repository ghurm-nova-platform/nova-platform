package ai.nova.platform.tool.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.tool.entity.ToolDefinition;
import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.entity.ToolType;

public interface ToolDefinitionRepository extends JpaRepository<ToolDefinition, UUID> {

    Optional<ToolDefinition> findByIdAndProjectIdAndOrganizationId(
            UUID id, UUID projectId, UUID organizationId);

    boolean existsByProjectIdAndToolKeyIgnoreCase(UUID projectId, String toolKey);

    boolean existsByProjectIdAndToolKeyIgnoreCaseAndIdNot(UUID projectId, String toolKey, UUID id);

    @Query("""
            SELECT t FROM ToolDefinition t
            WHERE t.organizationId = :organizationId
              AND t.projectId = :projectId
              AND (:status IS NULL OR t.status = :status)
              AND (:type IS NULL OR t.toolType = :type)
              AND (
                   :search IS NULL
                   OR LOWER(t.toolKey) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(t.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(COALESCE(t.description, '')) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              )
            """)
    Page<ToolDefinition> search(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("search") String search,
            @Param("status") ToolStatus status,
            @Param("type") ToolType type,
            Pageable pageable);
}
