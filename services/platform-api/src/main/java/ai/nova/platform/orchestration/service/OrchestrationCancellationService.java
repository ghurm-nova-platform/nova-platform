package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.RunResponse;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskAttempt;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.mapper.OrchestrationMapper;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskAttemptRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class OrchestrationCancellationService {

    private static final Set<TaskStatus> IMMEDIATE_CANCEL = EnumSet.of(
            TaskStatus.DRAFT,
            TaskStatus.BLOCKED,
            TaskStatus.READY,
            TaskStatus.RETRY_WAIT,
            TaskStatus.WAITING_APPROVAL,
            TaskStatus.CLAIMED);

    private static final Set<TaskStatus> TERMINAL = EnumSet.of(
            TaskStatus.SUCCEEDED,
            TaskStatus.FAILED,
            TaskStatus.SKIPPED,
            TaskStatus.CANCELLED,
            TaskStatus.TIMED_OUT);

    private final AgentOrchestrationRunRepository runRepository;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentTaskAttemptRepository attemptRepository;
    private final OrchestrationStateMachine stateMachine;
    private final OrchestrationEventService eventService;
    private final OrchestrationRunFinalizer runFinalizer;
    private final OrchestrationMapper mapper;
    private final AgentRuntimeClient agentRuntimeClient;
    private final Clock clock;

    public OrchestrationCancellationService(
            AgentOrchestrationRunRepository runRepository,
            AgentOrchestrationTaskRepository taskRepository,
            AgentTaskAttemptRepository attemptRepository,
            OrchestrationStateMachine stateMachine,
            OrchestrationEventService eventService,
            OrchestrationRunFinalizer runFinalizer,
            OrchestrationMapper mapper,
            AgentRuntimeClient agentRuntimeClient,
            Clock clock) {
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.attemptRepository = attemptRepository;
        this.stateMachine = stateMachine;
        this.eventService = eventService;
        this.runFinalizer = runFinalizer;
        this.mapper = mapper;
        this.agentRuntimeClient = agentRuntimeClient;
        this.clock = clock;
    }

    @Transactional
    public RunResponse cancelRun(UUID runId, String reason, AuthenticatedUser user) {
        AgentOrchestrationRun run = runRepository
                .findByIdAndOrganizationId(runId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORCHESTRATION_RUN_NOT_FOUND", "Run not found"));

        if (run.getStatus() == RunStatus.CANCELLED
                || run.getStatus() == RunStatus.ARCHIVED
                || run.getStatus() == RunStatus.CANCEL_REQUESTED) {
            if (run.getStatus() == RunStatus.CANCEL_REQUESTED) {
                runFinalizer.finalizeIfNeeded(run);
            }
            return mapper.toRunResponse(
                    run, taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId()));
        }

        Instant now = Instant.now(clock);
        String sanitized = sanitizeReason(reason);

        if (run.getStatus() == RunStatus.DRAFT) {
            stateMachine.transitionRun(run.getStatus(), RunStatus.CANCELLED);
            run.setStatus(RunStatus.CANCELLED);
            run.setCancelledAt(now);
            run.setCompletedAt(now);
            run.setCancellationReason(sanitized);
            run.setUpdatedBy(user.getUserId());
            run.setUpdatedAt(now);
            eventService.appendEvent(run, null, OrchestrationEventType.RUN_CANCELLED, null, user.getUserId());
            cancelAllTasks(run, sanitized, now, user.getUserId());
            runRepository.save(run);
            return mapper.toRunResponse(
                    run, taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId()));
        }

        stateMachine.transitionRun(run.getStatus(), RunStatus.CANCEL_REQUESTED);
        run.setStatus(RunStatus.CANCEL_REQUESTED);
        run.setCancellationReason(sanitized);
        run.setUpdatedBy(user.getUserId());
        run.setUpdatedAt(now);
        eventService.appendEvent(run, null, OrchestrationEventType.RUN_CANCEL_REQUESTED, null, user.getUserId());
        cancelAllTasks(run, sanitized, now, user.getUserId());
        runRepository.save(run);
        runFinalizer.finalizeIfNeeded(run);
        return mapper.toRunResponse(
                run, taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId()));
    }

    private void cancelAllTasks(AgentOrchestrationRun run, String reason, Instant now, UUID actor) {
        List<AgentOrchestrationTask> tasks =
                taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId());
        for (AgentOrchestrationTask task : tasks) {
            if (TERMINAL.contains(task.getStatus())) {
                continue;
            }
            if (IMMEDIATE_CANCEL.contains(task.getStatus()) || task.getStatus() == TaskStatus.CANCEL_REQUESTED) {
                if (task.getStatus() != TaskStatus.CANCELLED) {
                    if (task.getStatus() != TaskStatus.CANCEL_REQUESTED) {
                        stateMachine.transitionTask(task.getStatus(), TaskStatus.CANCEL_REQUESTED);
                        task.setStatus(TaskStatus.CANCEL_REQUESTED);
                        eventService.appendEvent(
                                run, task.getId(), OrchestrationEventType.TASK_CANCEL_REQUESTED, null, actor);
                    }
                    stateMachine.transitionTask(task.getStatus(), TaskStatus.CANCELLED);
                    task.setStatus(TaskStatus.CANCELLED);
                    task.setCancelledAt(now);
                    task.setCompletedAt(now);
                    task.setErrorCode("CANCELLED");
                    task.setErrorMessage(reason);
                    task.setUpdatedAt(now);
                    taskRepository.save(task);
                    eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_CANCELLED, null, actor);
                }
            } else if (task.getStatus() == TaskStatus.RUNNING) {
                stateMachine.transitionTask(task.getStatus(), TaskStatus.CANCEL_REQUESTED);
                task.setStatus(TaskStatus.CANCEL_REQUESTED);
                task.setUpdatedAt(now);
                taskRepository.save(task);
                eventService.appendEvent(
                        run, task.getId(), OrchestrationEventType.TASK_CANCEL_REQUESTED, null, actor);
                cancelExternalExecution(task);
            }
        }
    }

    private void cancelExternalExecution(AgentOrchestrationTask task) {
        attemptRepository.findByTaskIdAndOrganizationIdOrderByAttemptNumberAsc(task.getId(), task.getOrganizationId())
                .stream()
                .filter(a -> a.getExecutionId() != null)
                .map(AgentTaskAttempt::getExecutionId)
                .reduce((first, second) -> second)
                .ifPresent(agentRuntimeClient::cancel);
    }

    private static String sanitizeReason(String reason) {
        if (reason == null || reason.isBlank()) {
            return "Cancelled";
        }
        String trimmed = reason.trim();
        return trimmed.length() > 500 ? trimmed.substring(0, 500) : trimmed;
    }
}
