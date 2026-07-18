package ai.nova.platform.orchestration.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateDependencyRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.DeleteDependencyRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.DependencyResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.GraphResponse;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskDependency;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.mapper.OrchestrationMapper;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.repository.AgentTaskDependencyRepository;
import ai.nova.platform.orchestration.security.OrchestrationAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class OrchestrationGraphService {

    private final AgentOrchestrationRunRepository runRepository;
    private final AgentOrchestrationTaskRepository taskRepository;
    private final AgentTaskDependencyRepository dependencyRepository;
    private final OrchestrationAuthorizationService authorizationService;
    private final OrchestrationEventService eventService;
    private final OrchestrationMapper mapper;
    private final Clock clock;

    public OrchestrationGraphService(
            AgentOrchestrationRunRepository runRepository,
            AgentOrchestrationTaskRepository taskRepository,
            AgentTaskDependencyRepository dependencyRepository,
            OrchestrationAuthorizationService authorizationService,
            OrchestrationEventService eventService,
            OrchestrationMapper mapper,
            Clock clock) {
        this.runRepository = runRepository;
        this.taskRepository = taskRepository;
        this.dependencyRepository = dependencyRepository;
        this.authorizationService = authorizationService;
        this.eventService = eventService;
        this.mapper = mapper;
        this.clock = clock;
    }

    @Transactional
    public DependencyResponse addDependency(UUID runId, CreateDependencyRequest request, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_TASK_MANAGE);
        AgentOrchestrationRun run = requireDraftRun(runId, user.getOrganizationId());
        if (request.predecessorTaskId().equals(request.successorTaskId())) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ORCHESTRATION_SELF_DEPENDENCY", "Task cannot depend on itself");
        }
        AgentOrchestrationTask pred = requireTask(runId, request.predecessorTaskId(), user.getOrganizationId());
        AgentOrchestrationTask succ = requireTask(runId, request.successorTaskId(), user.getOrganizationId());
        if (!pred.getRunId().equals(run.getId()) || !succ.getRunId().equals(run.getId())) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "ORCHESTRATION_CROSS_RUN_DEPENDENCY", "Tasks must belong to the same run");
        }
        if (dependencyRepository.existsByPredecessorTaskIdAndSuccessorTaskId(
                request.predecessorTaskId(), request.successorTaskId())) {
            throw new ApiException(HttpStatus.CONFLICT, "DEPENDENCY_EXISTS", "Dependency already exists");
        }
        Instant now = Instant.now(clock);
        AgentTaskDependency dep = new AgentTaskDependency(
                run.getId(),
                request.predecessorTaskId(),
                request.successorTaskId(),
                request.dependencyType(),
                now);
        dependencyRepository.save(dep);
        eventService.appendEvent(run, null, OrchestrationEventType.DEPENDENCY_ADDED, null, user.getUserId());
        return mapper.toDependencyResponse(dep);
    }

    @Transactional
    public void removeDependency(UUID runId, DeleteDependencyRequest request, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_TASK_MANAGE);
        AgentOrchestrationRun run = requireDraftRun(runId, user.getOrganizationId());
        dependencyRepository.deleteByRunIdAndPredecessorTaskIdAndSuccessorTaskId(
                runId, request.predecessorTaskId(), request.successorTaskId());
        eventService.appendEvent(run, null, OrchestrationEventType.DEPENDENCY_REMOVED, null, user.getUserId());
    }

    @Transactional(readOnly = true)
    public GraphResponse getGraph(UUID runId, AuthenticatedUser user) {
        authorizationService.require(user, OrchestrationAuthorizationService.ORCHESTRATION_RUN_READ);
        requireRun(runId, user.getOrganizationId());
        List<AgentOrchestrationTask> tasks =
                taskRepository.findByRunIdAndOrganizationIdOrderBySequenceOrderAscCreatedAtAsc(
                        runId, user.getOrganizationId());
        List<AgentTaskDependency> deps = dependencyRepository.findByRunId(runId);
        return mapper.toGraphResponse(runId, tasks, deps);
    }

    private AgentOrchestrationRun requireDraftRun(UUID runId, UUID organizationId) {
        AgentOrchestrationRun run = requireRun(runId, organizationId);
        if (run.getStatus() != RunStatus.DRAFT) {
            throw new ApiException(
                    HttpStatus.CONFLICT, "INVALID_STATE_TRANSITION", "Dependencies can only be modified in DRAFT");
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
}
