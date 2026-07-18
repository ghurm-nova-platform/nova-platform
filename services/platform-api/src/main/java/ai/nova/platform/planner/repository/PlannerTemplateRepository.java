package ai.nova.platform.planner.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.planner.entity.PlannerTemplate;

public interface PlannerTemplateRepository extends JpaRepository<PlannerTemplate, UUID> {

    Optional<PlannerTemplate> findByIdAndOrganizationId(UUID id, UUID organizationId);

    @Query("""
            SELECT t FROM PlannerTemplate t
            WHERE t.organizationId = :organizationId
              AND t.enabled = TRUE
              AND (t.projectId IS NULL OR t.projectId = :projectId)
            ORDER BY CASE WHEN t.projectId IS NULL THEN 1 ELSE 0 END, t.name ASC
            """)
    List<PlannerTemplate> findEnabledForProject(
            @Param("organizationId") UUID organizationId, @Param("projectId") UUID projectId);
}
