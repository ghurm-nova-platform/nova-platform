package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.repository.AgentRepository;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.AttemptResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateTaskRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.TaskResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.UpdateTaskRequest;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.mapper.OrchestrationMapper;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskAttemptRepository;
import ai.nova.platform.orchestration.repository.AgentTaskDependencyRepository;
import ai.nova.platform.orchestration.security.OrchestrationAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class OrchestrationTaskService {

    private static final Pattern TASK_KEY = Pattern.compile("^[a-z0-9][a-z0-9._:-]{0,149}$");

    private final AgentOrchestrationRunRepository runRepository;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentTaskAttemptRepository attemptRepository;
    private final AgentTaskDependencyRepository dependencyRepository;
    private final AgentRepository agentRepository;
    private final OrchestrationAuthorizationService authorizationService;
    private final OrchestrationEventService eventService;
    private final OrchestrationMapper mapper;
    private final OrchestrationProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;
    private final AuditRecordingSupport auditRecordingSupport;

    public OrchestrationTaskService(
            AgentOrchestrationRunRepository runRepository,
            AgentOrchestrationTaskRepository taskRepository,
            AgentTaskAttemptRepository attemptRepository,
            AgentTaskDependencyRepository dependencyRepository,
            AgentRepository agentRepository,
            OrchestrationAuthorizationService authorizationService,
            OrchestrationEventService eventService,
            OrchestrationMapper mapper,
            OrchestrationProperties properties,
            ObjectMapper objectMapper,
            Clock clock,
            AuditRecordingSupport auditRecordingSupport) {
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.attemptRepository = attemptRepository;
        this.dependencyRepository = dependencyRepository;
        this.agentRepository = agentRepository;
        this.authorizationService = authorizationService;
        this.eventService = eventService;
        this.mapper = mapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
        this.auditRecordingSupport = auditRecordingSupport;
    }

    @Transactional
    public TaskResponse create(UUID runId, CreateTaskRequest request, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_TASK_MANAGE);
        AgentOrchestrationRun run = requireDraftRun(runId, user.getOrganizationId());
        validateTaskKey(request.taskKey());
        validateJsonOptional(request.inputJson());
        validateJsonOptional(request.requiredCapabilitiesJson());
        if (taskRepository.existsByRunIdAndTaskKey(runId, request.taskKey())) {
            throw new ApiException(HttpStatus.CONFLICT, "TASK_KEY_EXISTS", "Task key already exists");
        }
        String idempotency = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? request.taskKey()
                : request.idempotencyKey().trim();
        if (taskRepository.existsByRunIdAndIdempotencyKey(runId, idempotency)) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_EXISTS", "Idempotency key already exists");
        }
        if (request.assignedAgentId() != null) {
            agentRepository
                    .findByIdAndProjectIdAndOrganizationId(
                            request.assignedAgentId(), run.getProjectId(), run.getOrganizationId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
        }

        Instant now = Instant.now(clock);
        int maxAttempts = request.maxAttempts() == null ? 1 : request.maxAttempts();
        long backoff = request.retryBackoffMs() == null ? 1000L : request.retryBackoffMs();
        int priority = request.priority() == null ? 100 : request.priority();
        int timeout = request.timeoutSeconds() == null ? 60 : request.timeoutSeconds();
        if (maxAttempts > properties.getMaximumTaskAttempts()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "maxAttempts exceeds platform maximum");
        }
        if (timeout > properties.getMaximumTaskTimeoutSeconds()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "timeoutSeconds exceeds platform maximum");
        }

        AgentOrchestrationTask task = new AgentOrchestrationTask(
                UUID.randomUUID(),
                run.getOrganizationId(),
                run.getProjectId(),
                run.getId(),
                request.taskKey().trim(),
                request.displayName().trim(),
                request.taskType(),
                TaskStatus.DRAFT,
                idempotency,
                maxAttempts,
                backoff,
                priority,
                timeout,
                user.getUserId(),
                now);
        task.setDescription(request.description());
        task.setAssignedAgentId(request.assignedAgentId());
        task.setModelReference(blankToNull(request.modelReference()));
        task.setRequiredCapabilitiesJson(request.requiredCapabilitiesJson());
        task.setInputJson(request.inputJson());
        task.setSequenceOrder(request.sequenceOrder());
        taskRepository.save(task);
        eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_CREATED, null, user.getUserId());
        publishAudit(
                user,
                task,
                AuditAction.CREATE,
                AuditResult.SUCCESS,
                Map.of("runId", run.getId().toString(), "taskKey", task.getTaskKey(), "status", task.getStatus().name()));
        return mapper.toTaskResponse(task);
    }

    @Transactional(readOnly = true)
    public TaskResponse get(UUID runId, UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_READ);
        requireRun(runId, user.getOrganizationId());
        return mapper.toTaskResponse(requireTask(runId, taskId, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public Page<TaskResponse> list(
            UUID runId,
            TaskStatus status,
            ai.nova.platform.orchestration.entity.TaskType taskType,
            Pageable pageable,
            AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_READ);
        requireRun(runId, user.getOrganizationId());
        return taskRepository
                .searchByRun(runId, user.getOrganizationId(), status, taskType, pageable)
                .map(mapper::toTaskResponse);
    }

    @Transactional
    public TaskResponse update(UUID runId, UUID taskId, UpdateTaskRequest request, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_TASK_MANAGE);
        AgentOrchestrationRun run = requireDraftRun(runId, user.getOrganizationId());
        AgentOrchestrationTask task = requireTask(runId, taskId, user.getOrganizationId());
        if (request.version() == null || !request.version().equals(task.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Task was modified by another request");
        }
        validateTaskKey(request.taskKey());
        validateJsonOptional(request.inputJson());
        validateJsonOptional(request.requiredCapabilitiesJson());
        if (taskRepository.existsByRunIdAndTaskKeyAndIdNot(runId, request.taskKey(), taskId)) {
            throw new ApiException(HttpStatus.CONFLICT, "TASK_KEY_EXISTS", "Task key already exists");
        }
        String idempotency = request.idempotencyKey() == null || request.idempotencyKey().isBlank()
                ? request.taskKey()
                : request.idempotencyKey().trim();
        if (taskRepository.existsByRunIdAndIdempotencyKeyAndIdNot(runId, idempotency, taskId)) {
            throw new ApiException(HttpStatus.CONFLICT, "IDEMPOTENCY_KEY_EXISTS", "Idempotency key already exists");
        }
        if (request.assignedAgentId() != null) {
            agentRepository
                    .findByIdAndProjectIdAndOrganizationId(
                            request.assignedAgentId(), run.getProjectId(), run.getOrganizationId())
                    .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "AGENT_NOT_FOUND", "Agent not found"));
        }

        Instant now = Instant.now(clock);
        task.setTaskKey(request.taskKey().trim());
        task.setDisplayName(request.displayName().trim());
        task.setDescription(request.description());
        task.setTaskType(request.taskType());
        task.setAssignedAgentId(request.assignedAgentId());
        task.setModelReference(blankToNull(request.modelReference()));
        task.setRequiredCapabilitiesJson(request.requiredCapabilitiesJson());
        task.setInputJson(request.inputJson());
        task.setMaxAttempts(request.maxAttempts());
        task.setRetryBackoffMs(request.retryBackoffMs());
        task.setPriority(request.priority());
        task.setTimeoutSeconds(request.timeoutSeconds());
        task.setSequenceOrder(request.sequenceOrder());
        task.setIdempotencyKey(idempotency);
        task.setUpdatedBy(user.getUserId());
        task.setUpdatedAt(now);
        try {
            taskRepository.saveAndFlush(task);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Task was modified by another request");
        }
        eventService.appendEvent(run, task.getId(), OrchestrationEventType.TASK_UPDATED, null, user.getUserId());
        publishAudit(
                user,
                task,
                AuditAction.UPDATE,
                AuditResult.SUCCESS,
                Map.of("runId", run.getId().toString(), "taskKey", task.getTaskKey(), "status", task.getStatus().name()));
        return mapper.toTaskResponse(task);
    }

    @Transactional
    public void delete(UUID runId, UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_TASK_MANAGE);
        requireDraftRun(runId, user.getOrganizationId());
        AgentOrchestrationTask task = requireTask(runId, taskId, user.getOrganizationId());
        if (attemptRepository.existsByTaskId(taskId)) {
            throw new ApiException(HttpStatus.CONFLICT, "TASK_HAS_ATTEMPTS", "Cannot delete task with attempts");
        }
        dependencyRepository.deleteByPredecessorTaskIdOrSuccessorTaskId(taskId, taskId);
        taskRepository.delete(task);
    }

    @Transactional(readOnly = true)
    public List<AttemptResponse> listAttempts(UUID runId, UUID taskId, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_READ);
        requireRun(runId, user.getOrganizationId());
        requireTask(runId, taskId, user.getOrganizationId());
        return attemptRepository
                .findByTaskIdAndOrganizationIdOrderByAttemptNumberAsc(taskId, user.getOrganizationId())
                .stream()
                .map(mapper::toAttemptResponse)
                .toList();
    }

    private AgentOrchestrationRun requireDraftRun(UUID runId, UUID organizationId) {
        AgentOrchestrationRun run = requireRun(runId, organizationId);
        if (run.getStatus() != RunStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "Tasks can only be modified in DRAFT");
        }
        return run;
    }

    private AgentOrchestrationRun requireRun(UUID runId, UUID organizationId) {
        return runRepository
                .findByIdAndOrganizationId(runId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORCHESTRATION_RUN_NOT_FOUND", "Run not found"));
    }

    private AgentOrchestrationTask requireTask(UUID runId, UUID taskId, UUID organizationId) {
        return taskRepository
                .findByIdAndRunIdAndOrganizationId(taskId, runId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORCHESTRATION_TASK_NOT_FOUND", "Task not found"));
    }

    private void validateTaskKey(String taskKey) {
        if (taskKey == null || !TASK_KEY.matcher(taskKey.trim()).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Invalid taskKey format");
        }
    }

    private void validateJsonOptional(String json) {
        if (json == null || json.isBlank()) {
            return;
        }
        if (json.length() > properties.getMaxJsonChars()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "JSON exceeds maximum size");
        }
        try {
            objectMapper.readTree(json);
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "Invalid JSON");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private void publishAudit(
            AuthenticatedUser user,
            AgentOrchestrationTask task,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        try {
            auditRecordingSupport.recordDomainEvent(
                    user,
                    task.getProjectId(),
                    AuditEntityType.TASK,
                    task.getId(),
                    task.getDisplayName(),
                    action,
                    result,
                    AuditSource.ORCHESTRATION,
                    details);
        } catch (RuntimeException ignored) {
            // Audit must not change task lifecycle outcomes.
        }
    }
}
