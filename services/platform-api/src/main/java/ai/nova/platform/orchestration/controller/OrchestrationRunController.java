package ai.nova.platform.orchestration.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import ai.nova.platform.orchestration.dto.OrchestrationDtos.AttemptResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CancelRunRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateDependencyRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateRunRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateTaskRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.DeleteDependencyRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.DependencyResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.EventResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.GraphResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.RunResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.TaskResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.UpdateRunRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.UpdateTaskRequest;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.orchestration.mapper.OrchestrationMapper;
import ai.nova.platform.orchestration.repository.AgentOrchestrationEventRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.security.OrchestrationAuthorizationService;
import ai.nova.platform.orchestration.service.OrchestrationGraphService;
import ai.nova.platform.orchestration.service.OrchestrationRunService;
import ai.nova.platform.orchestration.service.OrchestrationTaskService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@RestController
@RequestMapping("/api/orchestration-runs")
public class OrchestrationRunController {

    private final OrchestrationRunService runService;
    private final OrchestrationTaskService taskService;
    private final OrchestrationGraphService graphService;
    private final AgentOrchestrationEventRepository eventRepository;
    private final AgentOrchestrationRunRepository runRepository;
    private final OrchestrationAuthorizationService authorizationService;
    private final OrchestrationMapper mapper;

    public OrchestrationRunController(
            OrchestrationRunService runService,
            OrchestrationTaskService taskService,
            OrchestrationGraphService graphService,
            AgentOrchestrationEventRepository eventRepository,
            AgentOrchestrationRunRepository runRepository,
            OrchestrationAuthorizationService authorizationService,
            OrchestrationMapper mapper) {
        this.runService = runService;
        this.taskService = taskService;
        this.graphService = graphService;
        this.eventRepository = eventRepository;
        this.runRepository = runRepository;
        this.authorizationService = authorizationService;
        this.mapper = mapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RunResponse create(
            @Valid @RequestBody CreateRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return runService.create(request, user);
    }

    @GetMapping
    public Page<RunResponse> list(
            @RequestParam(required = false) UUID projectId,
            @RequestParam(required = false) RunStatus status,
            @RequestParam(required = false) ExecutionMode executionMode,
            @RequestParam(required = false) UUID createdBy,
            @RequestParam(required = false) String search,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return runService.list(projectId, status, executionMode, createdBy, search, pageable, user);
    }

    @GetMapping("/{runId}")
    public RunResponse get(@PathVariable UUID runId, @AuthenticationPrincipal AuthenticatedUser user) {
        return runService.get(runId, user);
    }

    @PutMapping("/{runId}")
    public RunResponse update(
            @PathVariable UUID runId,
            @Valid @RequestBody UpdateRunRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return runService.update(runId, request, user);
    }

    @PostMapping("/{runId}/ready")
    public RunResponse ready(@PathVariable UUID runId, @AuthenticationPrincipal AuthenticatedUser user) {
        return runService.ready(runId, user);
    }

    @PostMapping("/{runId}/start")
    public RunResponse start(@PathVariable UUID runId, @AuthenticationPrincipal AuthenticatedUser user) {
        return runService.start(runId, user);
    }

    @PostMapping("/{runId}/cancel")
    public RunResponse cancel(
            @PathVariable UUID runId,
            @RequestBody(required = false) CancelRunRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return runService.cancel(runId, request, user);
    }

    @PostMapping("/{runId}/archive")
    public RunResponse archive(@PathVariable UUID runId, @AuthenticationPrincipal AuthenticatedUser user) {
        return runService.archive(runId, user);
    }

    @PostMapping("/{runId}/tasks")
    @ResponseStatus(HttpStatus.CREATED)
    public TaskResponse createTask(
            @PathVariable UUID runId,
            @Valid @RequestBody CreateTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return taskService.create(runId, request, user);
    }

    @GetMapping("/{runId}/tasks")
    public Page<TaskResponse> listTasks(
            @PathVariable UUID runId,
            @RequestParam(required = false) TaskStatus status,
            @RequestParam(required = false) TaskType taskType,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return taskService.list(runId, status, taskType, pageable, user);
    }

    @GetMapping("/{runId}/tasks/{taskId}")
    public TaskResponse getTask(
            @PathVariable UUID runId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return taskService.get(runId, taskId, user);
    }

    @PutMapping("/{runId}/tasks/{taskId}")
    public TaskResponse updateTask(
            @PathVariable UUID runId,
            @PathVariable UUID taskId,
            @Valid @RequestBody UpdateTaskRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return taskService.update(runId, taskId, request, user);
    }

    @DeleteMapping("/{runId}/tasks/{taskId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(
            @PathVariable UUID runId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        taskService.delete(runId, taskId, user);
    }

    @GetMapping("/{runId}/tasks/{taskId}/attempts")
    public List<AttemptResponse> listAttempts(
            @PathVariable UUID runId,
            @PathVariable UUID taskId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return taskService.listAttempts(runId, taskId, user);
    }

    @PostMapping("/{runId}/dependencies")
    @ResponseStatus(HttpStatus.CREATED)
    public DependencyResponse addDependency(
            @PathVariable UUID runId,
            @Valid @RequestBody CreateDependencyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return graphService.addDependency(runId, request, user);
    }

    @DeleteMapping("/{runId}/dependencies")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeDependency(
            @PathVariable UUID runId,
            @Valid @RequestBody DeleteDependencyRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        graphService.removeDependency(runId, request, user);
    }

    @GetMapping("/{runId}/graph")
    public GraphResponse getGraph(@PathVariable UUID runId, @AuthenticationPrincipal AuthenticatedUser user) {
        return graphService.getGraph(runId, user);
    }

    @GetMapping("/{runId}/events")
    public Page<EventResponse> listEvents(
            @PathVariable UUID runId,
            @RequestParam(required = false) UUID taskId,
            Pageable pageable,
            @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_EVENT_READ);
        runRepository
                .findByIdAndOrganizationId(runId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "ORCHESTRATION_RUN_NOT_FOUND", "Run not found"));
        return eventRepository
                .search(runId, user.getOrganizationId(), taskId, pageable)
                .map(mapper::toEventResponse);
    }
}
