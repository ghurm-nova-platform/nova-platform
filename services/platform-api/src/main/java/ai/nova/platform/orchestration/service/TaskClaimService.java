package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
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

    @Transactional
    public List<AgentOrchestrationTask> claimReadyTasks(int limit) {
        Instant now = Instant.now(clock);
        Instant expiresAt = now.plusSeconds(properties.getClaimLeaseSeconds());
        List<AgentOrchestrationTask> candidates = taskRepository.findReadyForClaim(now, limit);
        List<AgentOrchestrationTask> claimed = new ArrayList<>();
        for (AgentOrchestrationTask candidate : candidates) {
            int updated = taskRepository.claimReadyTask(
                    candidate.getId(),
                    candidate.getVersion(),
                    properties.getWorkerId(),
                    now,
                    expiresAt);
            if (updated == 1) {
                AgentOrchestrationTask refreshed = taskRepository.findById(candidate.getId()).orElse(null);
                if (refreshed != null) {
                    runRepository
                            .findByIdAndOrganizationId(refreshed.getRunId(), refreshed.getOrganizationId())
                            .ifPresent(run -> eventService.appendEvent(
                                    run,
                                    refreshed.getId(),
                                    OrchestrationEventType.TASK_CLAIMED,
                                    "{\"workerId\":\"" + properties.getWorkerId() + "\"}",
                                    null));
                    claimed.add(refreshed);
                }
            }
        }
        return claimed;
    }
}
