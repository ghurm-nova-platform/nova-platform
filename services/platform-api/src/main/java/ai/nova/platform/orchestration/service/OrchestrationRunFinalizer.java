package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;

@Service
public class OrchestrationRunFinalizer {

    private static final Set<TaskStatus> TERMINAL = EnumSet.of(
            TaskStatus.SUCCEEDED,
            TaskStatus.FAILED,
            TaskStatus.SKIPPED,
            TaskStatus.CANCELLED,
            TaskStatus.TIMED_OUT);

    private static final Set<TaskStatus> ACTIVE = EnumSet.of(
            TaskStatus.READY,
            TaskStatus.CLAIMED,
            TaskStatus.RUNNING);

    private static final Set<TaskStatus> WAITING = EnumSet.of(
            TaskStatus.RETRY_WAIT,
            TaskStatus.WAITING_APPROVAL,
            TaskStatus.BLOCKED);

    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final OrchestrationStateMachine stateMachine;
    private final OrchestrationEventService eventService;
    private final Clock clock;

    public OrchestrationRunFinalizer(
            AgentOrchestrationTaskRepository taskRepository,
            AgentOrchestrationRunRepository runRepository,
            OrchestrationStateMachine stateMachine,
            OrchestrationEventService eventService,
            Clock clock) {
        this.taskRepository = taskRepository;
        this.runRepository = runRepository;
        this.stateMachine = stateMachine;
        this.eventService = eventService;
        this.clock = clock;
    }

    @Transactional
    public RunStatus finalizeIfNeeded(AgentOrchestrationRun run) {
        if (run.getStatus() == RunStatus.CANCEL_REQUESTED) {
            return finalizeCancellation(run);
        }
        if (run.getStatus() != RunStatus.RUNNING && run.getStatus() != RunStatus.WAITING) {
            return run.getStatus();
        }

        Instant now = Instant.now(clock);
        if (run.getDeadlineAt() != null && now.isAfter(run.getDeadlineAt())) {
            return transitionTo(run, RunStatus.TIMED_OUT, OrchestrationEventType.RUN_TIMED_OUT, now);
        }

        List<AgentOrchestrationTask> tasks =
                taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId());

        long succeeded = tasks.stream().filter(t -> t.getStatus() == TaskStatus.SUCCEEDED).count();
        long failed = tasks.stream()
                .filter(t -> t.getStatus() == TaskStatus.FAILED || t.getStatus() == TaskStatus.TIMED_OUT)
                .count();
        long skipped = tasks.stream().filter(t -> t.getStatus() == TaskStatus.SKIPPED).count();
        long cancelled = tasks.stream().filter(t -> t.getStatus() == TaskStatus.CANCELLED).count();
        boolean anyActive = tasks.stream().anyMatch(t -> ACTIVE.contains(t.getStatus()));
        boolean anyWaiting = tasks.stream().anyMatch(t -> WAITING.contains(t.getStatus()));
        boolean allTerminal = tasks.stream().allMatch(t -> TERMINAL.contains(t.getStatus()));

        if (anyActive) {
            if (run.getStatus() == RunStatus.WAITING) {
                return transitionTo(run, RunStatus.RUNNING, OrchestrationEventType.RUN_STARTED, now);
            }
            return run.getStatus();
        }

        if (!allTerminal && anyWaiting) {
            if (run.getStatus() != RunStatus.WAITING) {
                return transitionTo(run, RunStatus.WAITING, OrchestrationEventType.RUN_WAITING, now);
            }
            return run.getStatus();
        }

        if (!allTerminal) {
            return run.getStatus();
        }

        if (cancelled > 0 && failed == 0 && succeeded == 0) {
            return transitionTo(run, RunStatus.CANCELLED, OrchestrationEventType.RUN_CANCELLED, now);
        }

        if (failed == 0 && succeeded + skipped == tasks.size()) {
            return transitionTo(run, RunStatus.SUCCEEDED, OrchestrationEventType.RUN_SUCCEEDED, now);
        }

        if (run.getFailurePolicy() == FailurePolicy.FAIL_FAST && failed > 0) {
            return transitionTo(run, RunStatus.FAILED, OrchestrationEventType.RUN_FAILED, now);
        }

        if (succeeded > 0 && failed > 0) {
            return transitionTo(
                    run, RunStatus.PARTIALLY_SUCCEEDED, OrchestrationEventType.RUN_PARTIALLY_SUCCEEDED, now);
        }

        return transitionTo(run, RunStatus.FAILED, OrchestrationEventType.RUN_FAILED, now);
    }

    private RunStatus finalizeCancellation(AgentOrchestrationRun run) {
        List<AgentOrchestrationTask> tasks =
                taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId());
        boolean anyActive = tasks.stream()
                .anyMatch(t -> t.getStatus() == TaskStatus.RUNNING || t.getStatus() == TaskStatus.CLAIMED);
        if (anyActive) {
            return run.getStatus();
        }
        Instant now = Instant.now(clock);
        return transitionTo(run, RunStatus.CANCELLED, OrchestrationEventType.RUN_CANCELLED, now);
    }

    private RunStatus transitionTo(
            AgentOrchestrationRun run, RunStatus target, OrchestrationEventType event, Instant now) {
        stateMachine.transitionRun(run.getStatus(), target);
        run.setStatus(target);
        if (target == RunStatus.RUNNING && run.getStartedAt() == null) {
            run.setStartedAt(now);
        }
        if (isTerminal(target)) {
            run.setCompletedAt(now);
            if (target == RunStatus.CANCELLED && run.getCancelledAt() == null) {
                run.setCancelledAt(now);
            }
        }
        run.setUpdatedAt(now);
        eventService.appendEvent(run, null, event, null, null);
        runRepository.save(run);
        return target;
    }

    private static boolean isTerminal(RunStatus status) {
        return status == RunStatus.SUCCEEDED
                || status == RunStatus.PARTIALLY_SUCCEEDED
                || status == RunStatus.FAILED
                || status == RunStatus.CANCELLED
                || status == RunStatus.TIMED_OUT
                || status == RunStatus.ARCHIVED;
    }
}
