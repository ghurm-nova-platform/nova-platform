package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;

@Service
public class TaskClaimService {

    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final OrchestrationEventService eventService;
    private final OrchestrationStateMachine stateMachine;
    private final OrchestrationProperties properties;
    private final Clock clock;

    public TaskClaimService(
            AgentOrchestrationTaskRepository taskRepository,
            AgentOrchestrationRunRepository runRepository,
            OrchestrationEventService eventService,
            OrchestrationStateMachine stateMachine,
            OrchestrationProperties properties,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.runRepository = runRepository;
        this.eventService = eventService;
        this.stateMachine = stateMachine;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public int reclaimExpiredClaims(int limit) {
        Instant now = Instant.now(clock);
        List<AgentOrchestrationTask> expired = taskRepository.findExpiredClaims(now, limit);
        int reclaimed = 0;
        for (AgentOrchestrationTask task : expired) {
            stateMachine.transitionTask(TaskStatus.CLAIMED, TaskStatus.READY);
            int updated = taskRepository.reclaimExpiredClaim(task.getId(), task.getVersion(), now);
            if (updated == 1) {
                reclaimed++;
            }
        }
        return reclaimed;
    }

    @Transactional
    public int promoteDueRetries(int limit) {
        Instant now = Instant.now(clock);
        List<AgentOrchestrationTask> due = taskRepository.findDueRetries(now, limit);
        int promoted = 0;
        for (AgentOrchestrationTask task : due) {
            stateMachine.transitionTask(TaskStatus.RETRY_WAIT, TaskStatus.READY);
            int updated = taskRepository.promoteDueRetry(task.getId(), task.getVersion(), now);
            if (updated == 1) {
                promoted++;
            }
        }
        return promoted;
    }

    /**
     * Claims READY tasks under a short global claim lock plus per-run pessimistic locks so
     * multi-node workers cannot exceed per-run {@code max_parallel_tasks} or global concurrency.
     * CLAIMED and RUNNING both consume capacity. External AI work must happen outside this TX.
     */
    @Transactional
    public List<AgentOrchestrationTask> claimReadyTasks(int limit) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(properties.getClaimLeaseSeconds());

        runRepository.lockGlobalClaimCapacity();

        long globalActive = taskRepository.countActiveSlotsGlobal();
        int globalLimit = Math.max(1, properties.getGlobalConcurrency());
        int globalRemaining = (int) Math.max(0, globalLimit - globalActive);
        if (globalRemaining == 0 || limit <= 0) {
            return List.of();
        }

        int fetchLimit = Math.min(limit, globalRemaining);
        List<AgentOrchestrationTask> candidates = taskRepository.findReadyForClaim(now, fetchLimit);
        List<AgentOrchestrationTask> claimed = new ArrayList<>();
        Set<UUID> lockedRuns = new HashSet<>();

        for (AgentOrchestrationTask candidate : candidates) {
            if (claimed.size() >= globalRemaining) {
                break;
            }

            UUID runId = candidate.getRunId();
            if (!lockedRuns.contains(runId)) {
                AgentOrchestrationRun run = runRepository.findByIdForUpdate(runId).orElse(null);
                lockedRuns.add(runId);
                if (run == null || !isExecutableRun(run.getStatus())) {
                    continue;
                }
            } else {
                AgentOrchestrationRun run = runRepository.findById(runId).orElse(null);
                if (run == null || !isExecutableRun(run.getStatus())) {
                    continue;
                }
            }

            AgentOrchestrationRun run = runRepository.findById(runId).orElseThrow();
            long runActive = taskRepository.countActiveSlotsByRunId(runId);
            if (runActive >= run.getMaxParallelTasks()) {
                continue;
            }

            int updated = taskRepository.claimReadyTask(
                    candidate.getId(),
                    candidate.getVersion(),
                    properties.getWorkerId(),
                    now,
                    expiresAt);
            if (updated == 1) {
                AgentOrchestrationTask refreshed = taskRepository.findById(candidate.getId()).orElse(null);
                if (refreshed != null) {
                    eventService.appendEvent(
                            run,
                            refreshed.getId(),
                            OrchestrationEventType.TASK_CLAIMED,
                            "{\"workerId\":\"" + properties.getWorkerId() + "\"}",
                            null);
                    claimed.add(refreshed);
                }
            }
        }
        return claimed;
    }

    /**
     * Releases a CLAIMED-but-not-started task back to READY (executor rejection / dispatch abort).
     * Does not create attempts and does not duplicate prior claim attempts.
     */
    @Transactional
    public boolean releaseUnstartedClaim(UUID taskId) {
        AgentOrchestrationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != TaskStatus.CLAIMED || task.getStartedAt() != null) {
            return false;
        }
        Instant now = Instant.now(clock);
        int updated = taskRepository.releaseUnstartedClaim(task.getId(), task.getVersion(), now);
        return updated == 1;
    }

    private static boolean isExecutableRun(RunStatus status) {
        return status == RunStatus.RUNNING || status == RunStatus.WAITING;
    }
}
