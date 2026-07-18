package ai.nova.platform.modelgateway.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.modelgateway.entity.ProjectModel;

import jakarta.persistence.LockModeType;

public interface ProjectModelRepository extends JpaRepository<ProjectModel, UUID> {

    Optional<ProjectModel> findByIdAndProjectIdAndOrganizationId(UUID id, UUID projectId, UUID organizationId);

    Optional<ProjectModel> findByProjectIdAndModelIdAndOrganizationId(
            UUID projectId, UUID modelId, UUID organizationId);

    List<ProjectModel> findByProjectIdAndOrganizationIdOrderByCreatedAtAsc(UUID projectId, UUID organizationId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT pm FROM ProjectModel pm
            WHERE pm.projectId = :projectId
              AND pm.organizationId = :organizationId
              AND pm.isDefault = true
              AND pm.enabled = true
            """)
    List<ProjectModel> lockEnabledDefaults(
            @Param("projectId") UUID projectId, @Param("organizationId") UUID organizationId);

    long countByProjectIdAndOrganizationIdAndEnabledTrue(UUID projectId, UUID organizationId);
}
