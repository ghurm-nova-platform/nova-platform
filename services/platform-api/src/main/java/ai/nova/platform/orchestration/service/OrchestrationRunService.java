package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CancelRunRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateRunRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.RunResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.UpdateRunRequest;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskDependency;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.mapper.OrchestrationMapper;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskDependencyRepository;
import ai.nova.platform.orchestration.security.OrchestrationAuthorizationService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class OrchestrationRunService {

    private static final Pattern JSON_OBJECT_OR_ARRAY = Pattern.compile("^\\s*[\\[\\{].*[\\]\\}]\\s*$", Pattern.DOTALL);

    private final AgentOrchestrationRunRepository runRepository;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentTaskDependencyRepository dependencyRepository;
    private final ProjectRepository projectRepository;
    private final OrchestrationAuthorizationService authorizationService;
    private final OrchestrationStateMachine stateMachine;
    private final OrchestrationGraphValidator graphValidator;
    private final OrchestrationEventService eventService;
    private final OrchestrationSchedulingService schedulingService;
    private final OrchestrationCancellationService cancellationService;
    private final OrchestrationMapper mapper;
    private final OrchestrationProperties properties;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    public OrchestrationRunService(
            AgentOrchestrationRunRepository runRepository,
            AgentOrchestrationTaskRepository taskRepository,
            AgentTaskDependencyRepository dependencyRepository,
            ProjectRepository projectRepository,
            OrchestrationAuthorizationService authorizationService,
            OrchestrationStateMachine stateMachine,
            OrchestrationGraphValidator graphValidator,
            OrchestrationEventService eventService,
            OrchestrationSchedulingService schedulingService,
            OrchestrationCancellationService cancellationService,
            OrchestrationMapper mapper,
            OrchestrationProperties properties,
            ObjectMapper objectMapper,
            Clock clock) {
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.projectRepository = projectRepository;
        this.authorizationService = authorizationService;
        this.stateMachine = stateMachine;
        this.graphValidator = graphValidator;
        this.eventService = eventService;
        this.schedulingService = schedulingService;
        this.cancellationService = cancellationService;
        this.mapper = mapper;
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.clock = clock;
    }

    @Transactional
    public RunResponse create(CreateRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_CREATE);
        requireProject(request.projectId(), user.getOrganizationId());
        validateJsonOptional(request.inputJson());
        validateJsonOptional(request.metadataJson());

        Instant now = Instant.now(clock);
        int maxParallel = request.maxParallelTasks() == null ? 1 : request.maxParallelTasks();
        long maxDuration = request.maximumDurationMs() == null
                ? properties.getMaximumRunDurationMs()
                : request.maximumDurationMs();
        if (maxDuration > properties.getMaximumRunDurationMs()) {
            maxDuration = properties.getMaximumRunDurationMs();
        }

        AgentOrchestrationRun run = new AgentOrchestrationRun(
                UUID.randomUUID(),
                user.getOrganizationId(),
                request.projectId(),
                request.name().trim(),
                request.objective().trim(),
                RunStatus.DRAFT,
                request.executionMode(),
                request.failurePolicy(),
                maxParallel,
                maxDuration,
                user.getUserId(),
                now);
        run.setInitiatedByAgentId(request.initiatedByAgentId());
        run.setInputJson(request.inputJson());
        run.setMetadataJson(request.metadataJson());
        runRepository.save(run);
        eventService.appendEvent(run, null, OrchestrationEventType.RUN_CREATED, null, user.getUserId());
        return toResponse(run);
    }

    @Transactional(readOnly = true)
    public RunResponse get(UUID runId, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_READ);
        return toResponse(requireRun(runId, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public Page<RunResponse> list(
            UUID projectId,
            RunStatus status,
            ExecutionMode executionMode,
            UUID createdBy,
            String search,
            Pageable pageable,
            AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_READ);
        return runRepository
                .search(user.getOrganizationId(), projectId, status, executionMode, createdBy, blankToNull(search), pageable)
                .map(this::toResponse);
    }

    @Transactional
    public RunResponse update(UUID runId, UpdateRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_UPDATE);
        AgentOrchestrationRun run = requireRun(runId, user.getOrganizationId());
        if (run.getStatus() != RunStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "Only DRAFT runs can be updated");
        }
        assertVersion(run, request.version());
        validateJsonOptional(request.inputJson());
        validateJsonOptional(request.metadataJson());

        Instant now = Instant.now(clock);
        run.setName(request.name().trim());
        run.setObjective(request.objective().trim());
        run.setExecutionMode(request.executionMode());
        run.setFailurePolicy(request.failurePolicy());
        run.setMaxParallelTasks(request.maxParallelTasks());
        run.setMaximumDurationMs(Math.min(request.maximumDurationMs(), properties.getMaximumRunDurationMs()));
        run.setInitiatedByAgentId(request.initiatedByAgentId());
        run.setInputJson(request.inputJson());
        run.setMetadataJson(request.metadataJson());
        run.setUpdatedBy(user.getUserId());
        run.setUpdatedAt(now);
        return toResponse(save(run));
    }

    @Transactional
    public RunResponse ready(UUID runId, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_START);
        AgentOrchestrationRun run = requireRun(runId, user.getOrganizationId());
        if (run.getStatus() != RunStatus.DRAFT) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "Only DRAFT runs can become READY");
        }
        List<AgentOrchestrationTask> tasks =
                taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId());
        List<AgentTaskDependency> deps = dependencyRepository.findByRunId(run.getId());
        graphValidator.validate(run, tasks, deps);

        Instant now = Instant.now(clock);
        stateMachine.transitionRun(run.getStatus(), RunStatus.READY);
        run.setStatus(RunStatus.READY);
        run.setUpdatedBy(user.getUserId());
        run.setUpdatedAt(now);
        eventService.appendEvent(run, null, OrchestrationEventType.RUN_READY, null, user.getUserId());
        return toResponse(save(run));
    }

    @Transactional
    public RunResponse start(UUID runId, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_START);
        AgentOrchestrationRun run = requireRun(runId, user.getOrganizationId());
        if (run.getStatus() == RunStatus.RUNNING || run.getStatus() == RunStatus.WAITING) {
            return toResponse(run);
        }
        if (run.getStatus() != RunStatus.READY) {
            throw new ApiException(HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "Only READY runs can be started");
        }

        Instant now = Instant.now(clock);
        stateMachine.transitionRun(run.getStatus(), RunStatus.RUNNING);
        run.setStatus(RunStatus.RUNNING);
        run.setStartedAt(now);
        run.setDeadlineAt(now.plusMillis(run.getMaximumDurationMs()));
        run.setUpdatedBy(user.getUserId());
        run.setUpdatedAt(now);
        eventService.appendEvent(run, null, OrchestrationEventType.RUN_STARTED, null, user.getUserId());
        schedulingService.initializeRootTasks(run);
        return toResponse(save(run));
    }

    @Transactional
    public RunResponse cancel(UUID runId, CancelRunRequest request, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_CANCEL);
        return cancellationService.cancelRun(runId, request == null ? null : request.reason(), user);
    }

    @Transactional
    public RunResponse archive(UUID runId, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_ARCHIVE);
        AgentOrchestrationRun run = requireRun(runId, user.getOrganizationId());
        stateMachine.transitionRun(run.getStatus(), RunStatus.ARCHIVED);
        Instant now = Instant.now(clock);
        run.setStatus(RunStatus.ARCHIVED);
        run.setUpdatedBy(user.getUserId());
        run.setUpdatedAt(now);
        eventService.appendEvent(run, null, OrchestrationEventType.RUN_ARCHIVED, null, user.getUserId());
        return toResponse(save(run));
    }

    AgentOrchestrationRun requireRun(UUID runId, UUID organizationId) {
        return runRepository
                .findByIdAndOrganizationId(runId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORCHESTRATION_RUN_NOT_FOUND", "Run not found"));
    }

    private RunResponse toResponse(AgentOrchestrationRun run) {
        List<AgentOrchestrationTask> tasks =
                taskRepository.findByRunIdAndOrganizationId(run.getId(), run.getOrganizationId());
        return mapper.toRunResponse(run, tasks);
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }

    private void assertVersion(AgentOrchestrationRun run, Long version) {
        if (version == null || !version.equals(run.getVersion())) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Run was modified by another request");
        }
    }

    private AgentOrchestrationRun save(AgentOrchestrationRun run) {
        try {
            return runRepository.saveAndFlush(run);
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK_CONFLICT", "Run was modified by another request");
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
        if (!JSON_OBJECT_OR_ARRAY.matcher(json).matches()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "JSON must be object or array");
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
