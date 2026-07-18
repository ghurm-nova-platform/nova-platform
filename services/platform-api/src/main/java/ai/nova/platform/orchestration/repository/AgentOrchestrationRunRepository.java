package ai.nova.platform.orchestration.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.RunStatus;

public interface AgentOrchestrationRunRepository extends JpaRepository<AgentOrchestrationRun, UUID> {

    Optional<AgentOrchestrationRun> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<AgentOrchestrationRun> findByIdAndOrganizationIdAndProjectId(
            UUID id, UUID organizationId, UUID projectId);

    @Query("""
            SELECT r FROM AgentOrchestrationRun r
            WHERE r.organizationId = :organizationId
              AND (:projectId IS NULL OR r.projectId = :projectId)
              AND (:status IS NULL OR r.status = :status)
              AND (:executionMode IS NULL OR r.executionMode = :executionMode)
              AND (:createdBy IS NULL OR r.createdBy = :createdBy)
              AND (
                   :search IS NULL
                   OR LOWER(r.name) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
                   OR LOWER(r.objective) LIKE LOWER(CONCAT('%', CAST(:search AS string), '%'))
              )
            """)
    Page<AgentOrchestrationRun> search(
            @Param("organizationId") UUID organizationId,
            @Param("projectId") UUID projectId,
            @Param("status") RunStatus status,
            @Param("executionMode") ExecutionMode executionMode,
            @Param("createdBy") UUID createdBy,
            @Param("search") String search,
            Pageable pageable);
}
