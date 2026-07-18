package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.entity.Agent;
import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.execution.service.ExecutionLifecycleService;
import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskAttempt;
import ai.nova.platform.orchestration.entity.AttemptStatus;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskAttemptRepository;
import ai.nova.platform.web.error.ApiException;

@Service
public class OrchestrationExecutionService {

    private static final Logger log = LoggerFactory.getLogger(OrchestrationExecutionService.class);

    private final AgentOrchestrationRunRepository runRepository;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentTaskAttemptRepository attemptRepository;
    private final AgentRepository agentRepository;
    private final OrchestrationStateMachine stateMachine;
    private final OrchestrationEventService eventService;
    private final OrchestrationSchedulingService schedulingService;
    private final OrchestrationRunFinalizer runFinalizer;
    private final TaskRetryPolicy retryPolicy;
    private final TaskInputResolver inputResolver;
    private final ExecutionLifecycleService executionLifecycleService;
    private final AgentRuntimeClient agentRuntimeClient;
    private final OrchestrationProperties properties;
    private final ObjectMapper objectMapper;
    private final TransactionTemplate transactionTemplate;
    private final Clock clock;

    public OrchestrationExecutionService(
            AgentOrchestrationRunRepository runRepository,
            AgentOrchestrationTaskRepository taskRepository,
            AgentTaskAttemptRepository attemptRepository,
            AgentRepository agentRepository,
            OrchestrationStateMachine stateMachine,
            OrchestrationEventService eventService,
            OrchestrationSchedulingService schedulingService,
            OrchestrationRunFinalizer runFinalizer,
            TaskRetryPolicy retryPolicy,
            TaskInputResolver inputResolver,
            ExecutionLifecycleService executionLifecycleService,
            AgentRuntimeClient agentRuntimeClient,
            OrchestrationProperties properties,
            ObjectMapper objectMapper,
            TransactionTemplate transactionTemplate,
            Clock clock) {
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.attemptRepository = attemptRepository;
        this.agentRepository = agentRepository;
        this.stateMachine = stateMachine;
        this.eventService = eventService;
        this.schedulingService = schedulingService;
        this.runFinalizer = runFinalizer;
        this.retryPolicy = retryPolicy;
        this.inputResolver = inputResolver;
        this.executionLifecycleService = executionLifecycleService;
        this.agentRuntimeClient = agentRuntimeClient;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.transactionTemplate = transactionTemplate;
        this.clock = clock;
    }

    public void executeClaimedTask(UUID taskId) {
        Tx1Context ctx = transactionTemplate.execute(status -> beginExecution(taskId));
        if (ctx == null) {
            return;
        }
        try {
            if (ctx.unsupported()) {
                transactionTemplate.executeWithoutResult(status -> applyFailure(
                        ctx, "TASK_TYPE_UNSUPPORTED", "Task type is not executable in this phase", false));
                return;
            }
            RuntimeTurnResult result = agentRuntimeClient.execute(ctx.request());
            transactionTemplate.executeWithoutResult(status -> applySuccess(ctx, result));
        } catch (RuntimeException ex) {
            String code = ex instanceof ApiException api ? api.getCode() : "EXECUTION_FAILED";
            String message = ex instanceof ApiException api ? api.getMessage() : "Execution failed";
            boolean retryable = retryPolicy.isRetryable(code);
            log.warn("Orchestration task {} failed code={}", taskId, code);
            transactionTemplate.executeWithoutResult(status -> applyFailure(ctx, code, message, retryable));
        }
    }

    @Transactional
    protected Tx1Context beginExecution(UUID taskId) {
        AgentOrchestrationTask task = taskRepository.findById(taskId).orElse(null);
        if (task == null || task.getStatus() != TaskStatus.CLAIMED) {
            return null;
        }
        AgentOrchestrationRun run = runRepository
                .findByIdAndOrganizationId(task.getRunId(), task.getOrganizationId())
                .orElse(null);
        if (run == null) {
            return null;
        }
        if (run.getStatus() == RunStatus.CANCEL_REQUESTED
                || run.getStatus() == RunStatus.CANCELLED
                || run.getStatus() == RunStatus.TIMED_OUT
                || run.getStatus() == RunStatus.ARCHIVED) {
            return null;
        }

        Instant now = Instant.now(clock);
        long taskVersion = task.getVersion();
        long runVersion = run.getVersion();

        if (task.getTaskType() != TaskType.AGENT_TURN) {
            stateMachine.transitionTask(task.getStatus(), TaskStatus.RUNNING);
            task.setStatus(TaskStatus.RUNNING);
            task.setStartedAt(now);
            task.setUpdatedAt(now);
            int attemptNumber = task.getAttemptCount() + 1;
            task.setAttemptCount(attemptNumber);
            AgentTaskAttempt attempt = new AgentTaskAttempt(
                    UUID.randomUUID(),
                    task.getOrganizationId(),
                    task.getProjectId(),
                    task.getRunId(),
                    task.getId(),
                    attemptNumber,
                    AttemptStatus.STARTED,
                    now,
                    properties.getWorkerId(),
                    now);
            attemptRepository.save(attempt);
            taskRepository.save(task);
            eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_STARTED, null, null);
            return new Tx1Context(
                    task.getId(),
                    run.getId(),
                    task.getOrganizationId(),
                    attempt.getId(),
                    attemptNumber,
                    taskVersion + 1,
                    runVersion,
                    null,
                    true,
                    null);
        }

        Map<String, AgentOrchestrationTask> byKey = new HashMap<>();
        for (AgentOrchestrationTask t :
                taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId())) {
            byKey.put(t.getTaskKey(), t);
        }
        String resolvedInput = inputResolver.resolve(task.getInputJson(), byKey);

        Agent agent = null;
        if (task.getAssignedAgentId() != null) {
            agent = agentRepository
                    .findByIdAndProjectIdAndOrganizationId(
                            task.getAssignedAgentId(), task.getProjectId(), task.getOrganizationId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
        }

        UUID executionId = UUID.randomUUID();
        String systemPrompt = agent != null ? agent.getSystemPrompt() : "You are a helpful assistant.";
        String provider = agent != null ? agent.getModelProvider() : "LOCAL";
        String modelName = agent != null ? agent.getModelName() : "default";
        String modelReference =
                task.getModelReference() != null && !task.getModelReference().isBlank()
                        ? task.getModelReference().trim()
                        : null;
        UUID agentId = agent != null ? agent.getId() : task.getAssignedAgentId();
        if (agentId == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ORCHESTRATION_AGENT_REQUIRED", "Assigned agent is required");
        }

        String userMessage = buildUserMessage(run, task, resolvedInput);
        executionLifecycleService.startRunning(
                executionId,
                task.getOrganizationId(),
                task.getProjectId(),
                agentId,
                agent != null ? agent.getPromptVersionId() : null,
                null,
                provider,
                modelName,
                run.getCreatedBy(),
                systemPrompt,
                userMessage);

        stateMachine.transitionTask(task.getStatus(), TaskStatus.RUNNING);
        task.setStatus(TaskStatus.RUNNING);
        task.setStartedAt(now);
        task.setUpdatedAt(now);
        int attemptNumber = task.getAttemptCount() + 1;
        task.setAttemptCount(attemptNumber);
        AgentTaskAttempt attempt = new AgentTaskAttempt(
                UUID.randomUUID(),
                task.getOrganizationId(),
                task.getProjectId(),
                task.getRunId(),
                task.getId(),
                attemptNumber,
                AttemptStatus.STARTED,
                now,
                properties.getWorkerId(),
                now);
        attempt.setExecutionId(executionId);
        attempt.setInputSnapshotJson(truncate(resolvedInput));
        attemptRepository.save(attempt);
        taskRepository.save(task);
        eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_STARTED, null, null);

        ExecutionRequest request = new ExecutionRequest(
                task.getOrganizationId(),
                task.getProjectId(),
                agentId,
                executionId,
                provider,
                modelReference,
                systemPrompt,
                List.of(new RuntimeMessage("user", userMessage)),
                null,
                List.of(),
                List.of(),
                null);
        return new Tx1Context(
                task.getId(),
                run.getId(),
                task.getOrganizationId(),
                attempt.getId(),
                attemptNumber,
                task.getVersion(),
                run.getVersion(),
                request,
                false,
                executionId);
    }

    @Transactional
    protected void applySuccess(Tx1Context ctx, RuntimeTurnResult result) {
        AgentOrchestrationTask task = taskRepository
                .findByIdAndRunIdAndOrganizationId(ctx.taskId(), ctx.runId(), ctx.organizationId())
                .orElse(null);
        AgentOrchestrationRun run = runRepository
                .findByIdAndOrganizationId(ctx.runId(), ctx.organizationId())
                .orElse(null);
        if (task == null || run == null) {
            return;
        }
        if (isStaleOrCancelled(task, run, ctx)) {
            markStale(run, task, ctx);
            return;
        }

        Instant now = Instant.now(clock);
        String output = buildOutput(result);
        AgentTaskAttempt attempt = attemptRepository
                .findById(ctx.attemptId())
                .orElse(null);
        if (attempt == null || attempt.getAttemptNumber() != ctx.attemptNumber()) {
            markStale(run, task, ctx);
            return;
        }

        stateMachine.transitionTask(task.getStatus(), TaskStatus.SUCCEEDED);
        task.setStatus(TaskStatus.SUCCEEDED);
        task.setOutputJson(output);
        task.setCompletedAt(now);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setUpdatedAt(now);
        taskRepository.save(task);

        attempt.setStatus(AttemptStatus.SUCCEEDED);
        attempt.setCompletedAt(now);
        attempt.setDurationMs(Math.max(0, now.toEpochMilli() - attempt.getStartedAt().toEpochMilli()));
        attempt.setOutputSnapshotJson(output);
        attemptRepository.save(attempt);

        if (ctx.executionId() != null && result != null && result.isFinal() && result.finalResponse() != null) {
            executionLifecycleService.completeIfRunning(ctx.executionId(), result.finalResponse());
        }

        eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_SUCCEEDED, null, null);
        schedulingService.afterTaskTerminal(run, task);
        runFinalizer.finalizeIfNeeded(run);
    }

    @Transactional
    protected void applyFailure(Tx1Context ctx, String errorCode, String errorMessage, boolean retryable) {
        AgentOrchestrationTask task = taskRepository
                .findByIdAndRunIdAndOrganizationId(ctx.taskId(), ctx.runId(), ctx.organizationId())
                .orElse(null);
        AgentOrchestrationRun run = runRepository
                .findByIdAndOrganizationId(ctx.runId(), ctx.organizationId())
                .orElse(null);
        if (task == null || run == null) {
            return;
        }
        if (isStaleOrCancelled(task, run, ctx)) {
            markStale(run, task, ctx);
            return;
        }

        Instant now = Instant.now(clock);
        AgentTaskAttempt attempt = attemptRepository.findById(ctx.attemptId()).orElse(null);
        if (attempt != null) {
            attempt.setStatus(AttemptStatus.FAILED);
            attempt.setCompletedAt(now);
            attempt.setDurationMs(Math.max(0, now.toEpochMilli() - attempt.getStartedAt().toEpochMilli()));
            attempt.setErrorCode(errorCode);
            attempt.setErrorMessage(sanitize(errorMessage));
            attempt.setRetryable(retryable);
            attemptRepository.save(attempt);
        }
        if (ctx.executionId() != null) {
            executionLifecycleService.failIfRunning(ctx.executionId());
        }

        boolean canRetry = retryable && task.getAttemptCount() < task.getMaxAttempts();
        if (canRetry) {
            stateMachine.transitionTask(task.getStatus(), TaskStatus.RETRY_WAIT);
            task.setStatus(TaskStatus.RETRY_WAIT);
            long backoff = retryPolicy.nextBackoffMs(task.getAttemptCount(), task.getRetryBackoffMs());
            task.setNextAttemptAt(now.plusMillis(backoff));
            task.setErrorCode(errorCode);
            task.setErrorMessage(sanitize(errorMessage));
            task.setUpdatedAt(now);
            taskRepository.save(task);
            eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_RETRY_SCHEDULED, null, null);
        } else {
            stateMachine.transitionTask(task.getStatus(), TaskStatus.FAILED);
            task.setStatus(TaskStatus.FAILED);
            task.setCompletedAt(now);
            task.setErrorCode(errorCode);
            task.setErrorMessage(sanitize(errorMessage));
            task.setUpdatedAt(now);
            taskRepository.save(task);
            eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_FAILED, null, null);
            schedulingService.afterTaskTerminal(run, task);
        }
        runFinalizer.finalizeIfNeeded(run);
    }

    private boolean isStaleOrCancelled(AgentOrchestrationTask task, AgentOrchestrationRun run, Tx1Context ctx) {
        if (task.getStatus() == TaskStatus.CANCEL_REQUESTED
                || task.getStatus() == TaskStatus.CANCELLED
                || task.getStatus() == TaskStatus.SUCCEEDED
                || task.getStatus() == TaskStatus.FAILED
                || task.getStatus() == TaskStatus.TIMED_OUT
                || task.getStatus() == TaskStatus.SKIPPED) {
            return true;
        }
        if (run.getStatus() == RunStatus.CANCEL_REQUESTED
                || run.getStatus() == RunStatus.CANCELLED
                || run.getStatus() == RunStatus.TIMED_OUT) {
            return true;
        }
        if (task.getAttemptCount() != ctx.attemptNumber()) {
            return true;
        }
        return false;
    }

    private void markStale(AgentOrchestrationRun run, AgentOrchestrationTask task, Tx1Context ctx) {
        attemptRepository.findById(ctx.attemptId()).ifPresent(attempt -> {
            if (attempt.getStatus() == AttemptStatus.STARTED) {
                attempt.setStatus(AttemptStatus.STALE);
                attempt.setCompletedAt(Instant.now(clock));
                attemptRepository.save(attempt);
            }
        });
        if (task.getStatus() == TaskStatus.CANCEL_REQUESTED) {
            Instant now = Instant.now(clock);
            stateMachine.transitionTask(task.getStatus(), TaskStatus.CANCELLED);
            task.setStatus(TaskStatus.CANCELLED);
            task.setCancelledAt(now);
            task.setCompletedAt(now);
            task.setUpdatedAt(now);
            taskRepository.save(task);
            eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_CANCELLED, null, null);
        }
        eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_STALE_RESULT_IGNORED, null, null);
        runFinalizer.finalizeIfNeeded(run);
    }

    private String buildUserMessage(AgentOrchestrationRun run, AgentOrchestrationTask task, String resolvedInput) {
        StringBuilder sb = new StringBuilder();
        sb.append("Objective: ").append(run.getObjective()).append('\n');
        sb.append("Task: ").append(task.getDisplayName()).append('\n');
        if (task.getDescription() != null) {
            sb.append(task.getDescription()).append('\n');
        }
        if (resolvedInput != null && !resolvedInput.isBlank()) {
            sb.append("Input: ").append(resolvedInput);
        }
        return sb.toString();
    }

    private String buildOutput(RuntimeTurnResult result) {
        try {
            if (result != null && result.isFinal() && result.finalResponse() != null) {
                RuntimeFinalResponse fr = result.finalResponse();
                return objectMapper.writeValueAsString(Map.of(
                        "response", fr.responseText() == null ? "" : fr.responseText(),
                        "inputTokens", fr.inputTokens(),
                        "outputTokens", fr.outputTokens(),
                        "totalTokens", fr.totalTokens()));
            }
            return objectMapper.writeValueAsString(Map.of("response", ""));
        } catch (Exception ex) {
            return "{\"response\":\"\"}";
        }
    }

    private String truncate(String value) {
        if (value == null) {
            return null;
        }
        if (value.length() <= properties.getMaxJsonChars()) {
            return value;
        }
        return value.substring(0, properties.getMaxJsonChars());
    }

    private static String sanitize(String message) {
        if (message == null) {
            return "Execution failed";
        }
        String trimmed = message.trim();
        return trimmed.length() > 2000 ? trimmed.substring(0, 2000) : trimmed;
    }

    public record Tx1Context(
            UUID taskId,
            UUID runId,
            UUID organizationId,
            UUID attemptId,
            int attemptNumber,
            long taskVersion,
            long runVersion,
            ExecutionRequest request,
            boolean unsupported,
            UUID executionId) {
    }
}
