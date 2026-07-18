package ai.nova.platform.orchestration.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;

public interface AgentOrchestrationTaskRepository extends JpaRepository<AgentOrchestrationTask, UUID> {

    Optional<AgentOrchestrationTask> findByIdAndRunIdAndOrganizationId(UUID id, UUID runId, UUID organizationId);

    Optional<AgentOrchestrationTask> findByIdAndOrganizationId(UUID id, UUID organizationId);

    List<AgentOrchestrationTask> findByRunIdAndOrganizationId(UUID runId, UUID organizationId);

    List<AgentOrchestrationTask> findByRunIdAndOrganizationIdOrderBySequenceOrderAscCreatedAtAsc(
            UUID runId, UUID organizationId);

    boolean existsByRunIdAndTaskKey(UUID runId, String taskKey);

    boolean existsByRunIdAndIdempotencyKey(UUID runId, String idempotencyKey);

    boolean existsByRunIdAndTaskKeyAndIdNot(UUID runId, String taskKey, UUID id);

    boolean existsByRunIdAndIdempotencyKeyAndIdNot(UUID runId, String idempotencyKey, UUID id);

    long countByRunIdAndOrganizationIdAndStatusIn(UUID runId, UUID organizationId, List<TaskStatus> statuses);

    @Query(value = """
            SELECT COUNT(*) FROM agent_orchestration_tasks
            WHERE run_id = :runId
              AND status IN ('CLAIMED', 'RUNNING')
            """, nativeQuery = true)
    long countActiveSlotsByRunId(@Param("runId") UUID runId);

    @Query(value = """
            SELECT COUNT(*) FROM agent_orchestration_tasks
            WHERE status IN ('CLAIMED', 'RUNNING')
            """, nativeQuery = true)
    long countActiveSlotsGlobal();

    @Query("""
            SELECT t FROM AgentOrchestrationTask t
            WHERE t.runId = :runId
              AND t.organizationId = :organizationId
              AND (:status IS NULL OR t.status = :status)
              AND (:taskType IS NULL OR t.taskType = :taskType)
            """)
    Page<AgentOrchestrationTask> searchByRun(
            @Param("runId") UUID runId,
            @Param("organizationId") UUID organizationId,
            @Param("status") TaskStatus status,
            @Param("taskType") TaskType taskType,
            Pageable pageable);

    /**
     * Candidate READY tasks whose parent run is executable and currently under per-run capacity.
     * Authoritative capacity enforcement happens under claim locks in TaskClaimService.
     */
    @Query(value = """
            SELECT t.* FROM agent_orchestration_tasks t
            INNER JOIN agent_orchestration_runs r ON r.id = t.run_id
            WHERE t.status = 'READY'
              AND (t.next_attempt_at IS NULL OR t.next_attempt_at <= :now)
              AND r.status IN ('RUNNING', 'WAITING')
              AND (
                    SELECT COUNT(*) FROM agent_orchestration_tasks active
                    WHERE active.run_id = t.run_id
                      AND active.status IN ('CLAIMED', 'RUNNING')
                  ) < r.max_parallel_tasks
            ORDER BY t.priority ASC, t.created_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<AgentOrchestrationTask> findReadyForClaim(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE agent_orchestration_tasks
            SET status = 'CLAIMED',
                claimed_by = :workerId,
                claimed_at = :now,
                claim_expires_at = :expiresAt,
                updated_at = :now,
                version = version + 1
            WHERE id = :id
              AND status = 'READY'
              AND version = :version
              AND EXISTS (
                    SELECT 1 FROM agent_orchestration_runs r
                    WHERE r.id = agent_orchestration_tasks.run_id
                      AND r.status IN ('RUNNING', 'WAITING')
                  )
              AND (
                    SELECT COUNT(*) FROM agent_orchestration_tasks active
                    WHERE active.run_id = agent_orchestration_tasks.run_id
                      AND active.status IN ('CLAIMED', 'RUNNING')
                  ) < (
                    SELECT r.max_parallel_tasks FROM agent_orchestration_runs r
                    WHERE r.id = agent_orchestration_tasks.run_id
                  )
            """, nativeQuery = true)
    int claimReadyTask(
            @Param("id") UUID id,
            @Param("version") long version,
            @Param("workerId") String workerId,
            @Param("now") Instant now,
            @Param("expiresAt") Instant expiresAt);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE agent_orchestration_tasks
            SET status = 'READY',
                claimed_by = NULL,
                claimed_at = NULL,
                claim_expires_at = NULL,
                updated_at = :now,
                version = version + 1
            WHERE id = :id
              AND status = 'CLAIMED'
              AND started_at IS NULL
              AND version = :version
            """, nativeQuery = true)
    int releaseUnstartedClaim(
            @Param("id") UUID id,
            @Param("version") long version,
            @Param("now") Instant now);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE agent_orchestration_tasks
            SET status = 'READY',
                claimed_by = NULL,
                claimed_at = NULL,
                claim_expires_at = NULL,
                updated_at = :now,
                version = version + 1
            WHERE id = :id
              AND status = 'CLAIMED'
              AND claim_expires_at < :now
              AND started_at IS NULL
              AND version = :version
            """, nativeQuery = true)
    int reclaimExpiredClaim(
            @Param("id") UUID id,
            @Param("version") long version,
            @Param("now") Instant now);

    @Query(value = """
            SELECT * FROM agent_orchestration_tasks t
            WHERE t.status = 'CLAIMED'
              AND t.claim_expires_at < :now
              AND t.started_at IS NULL
            ORDER BY t.claim_expires_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<AgentOrchestrationTask> findExpiredClaims(@Param("now") Instant now, @Param("limit") int limit);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            UPDATE agent_orchestration_tasks
            SET status = 'READY',
                next_attempt_at = NULL,
                updated_at = :now,
                version = version + 1
            WHERE id = :id
              AND status = 'RETRY_WAIT'
              AND next_attempt_at IS NOT NULL
              AND next_attempt_at <= :now
              AND version = :version
            """, nativeQuery = true)
    int promoteDueRetry(
            @Param("id") UUID id,
            @Param("version") long version,
            @Param("now") Instant now);

    @Query(value = """
            SELECT * FROM agent_orchestration_tasks t
            WHERE t.status = 'RETRY_WAIT'
              AND t.next_attempt_at IS NOT NULL
              AND t.next_attempt_at <= :now
            ORDER BY t.next_attempt_at ASC
            LIMIT :limit
            """, nativeQuery = true)
    List<AgentOrchestrationTask> findDueRetries(@Param("now") Instant now, @Param("limit") int limit);
}
