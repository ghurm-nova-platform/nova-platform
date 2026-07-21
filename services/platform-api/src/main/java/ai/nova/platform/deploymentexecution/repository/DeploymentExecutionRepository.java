package ai.nova.platform.deploymentexecution.repository;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;

public interface DeploymentExecutionRepository extends JpaRepository<DeploymentExecutionEntity, UUID> {

    Optional<DeploymentExecutionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<DeploymentExecutionEntity> findByOrganizationIdAndExecutionFingerprint(
            UUID organizationId, String executionFingerprint);

    List<DeploymentExecutionEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<DeploymentExecutionEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
            UUID organizationId, UUID projectId);

    boolean existsByOrganizationIdAndEnvironmentIdAndStatusIn(
            UUID organizationId, UUID environmentId, Collection<ExecutionStatus> statuses);

    long countByOrganizationIdAndEnvironmentIdAndStatusIn(
            UUID organizationId, UUID environmentId, Collection<ExecutionStatus> statuses);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE DeploymentExecutionEntity e
            SET e.status = :starting,
                e.startedAt = :now,
                e.updatedAt = :now
            WHERE e.id = :id
              AND e.organizationId = :organizationId
              AND e.status = :queued
              AND e.cancelRequested = false
            """)
    int claimQueued(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId,
            @Param("queued") ExecutionStatus queued,
            @Param("starting") ExecutionStatus starting,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE DeploymentExecutionEntity e
            SET e.cancelRequested = true,
                e.updatedAt = :now
            WHERE e.id = :id
              AND e.organizationId = :organizationId
              AND e.cancelRequested = false
              AND e.status IN :activeStatuses
            """)
    int markCancelRequested(
            @Param("id") UUID id,
            @Param("organizationId") UUID organizationId,
            @Param("activeStatuses") Collection<ExecutionStatus> activeStatuses,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE DeploymentExecutionEntity e
            SET e.status = :cancelled,
                e.finishedAt = :now,
                e.updatedAt = :now,
                e.activeEnvironmentSlot = null
            WHERE e.id = :id
              AND e.cancelRequested = true
              AND e.status IN :activeStatuses
            """)
    int finalizeCancelled(
            @Param("id") UUID id,
            @Param("cancelled") ExecutionStatus cancelled,
            @Param("activeStatuses") Collection<ExecutionStatus> activeStatuses,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE DeploymentExecutionEntity e
            SET e.status = :nextStatus,
                e.currentStep = :step,
                e.currentStage = :stage,
                e.updatedAt = :now
            WHERE e.id = :id
              AND e.cancelRequested = false
              AND e.status = :expectedStatus
            """)
    int transitionIfActive(
            @Param("id") UUID id,
            @Param("expectedStatus") ExecutionStatus expectedStatus,
            @Param("nextStatus") ExecutionStatus nextStatus,
            @Param("step") String step,
            @Param("stage") String stage,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE DeploymentExecutionEntity e
            SET e.status = :completed,
                e.finishedAt = :now,
                e.updatedAt = :now,
                e.activeEnvironmentSlot = null
            WHERE e.id = :id
              AND e.cancelRequested = false
              AND e.status = :verifying
            """)
    int completeIfNotCancelled(
            @Param("id") UUID id,
            @Param("verifying") ExecutionStatus verifying,
            @Param("completed") ExecutionStatus completed,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
            """
            UPDATE DeploymentExecutionEntity e
            SET e.status = :failed,
                e.errorCode = :errorCode,
                e.errorMessage = :errorMessage,
                e.finishedAt = :now,
                e.updatedAt = :now,
                e.activeEnvironmentSlot = null
            WHERE e.id = :id
              AND e.status IN :activeStatuses
            """)
    int failIfActive(
            @Param("id") UUID id,
            @Param("failed") ExecutionStatus failed,
            @Param("activeStatuses") Collection<ExecutionStatus> activeStatuses,
            @Param("errorCode") String errorCode,
            @Param("errorMessage") String errorMessage,
            @Param("now") Instant now);
}
