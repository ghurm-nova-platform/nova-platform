package ai.nova.platform.planner.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateDependencyRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateRunRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.CreateTaskRequest;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.RunResponse;
import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.service.OrchestrationGraphService;
import ai.nova.platform.orchestration.service.OrchestrationRunService;
import ai.nova.platform.orchestration.service.OrchestrationTaskService;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionDependency;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionPlan;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionTaskDefinition;
import ai.nova.platform.planner.dto.PlannerDtos.ImportPlanRequest;
import ai.nova.platform.planner.dto.PlannerDtos.PlanAndCreateResponse;
import ai.nova.platform.planner.dto.PlannerDtos.PlannerRequest;
import ai.nova.platform.planner.dto.PlannerDtos.PlannerResponse;
import ai.nova.platform.planner.security.PlannerAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Imports a validated {@link ExecutionPlan} into a DRAFT orchestration run (tasks + dependencies).
 * Does not mark ready or start execution.
 */
@Service
public class PlannerImportService {

    private final PlannerAuthorizationService authorizationService;
    private final PlannerPlanValidator planValidator;
    private final PlannerService plannerService;
    private final OrchestrationRunService runService;
    private final OrchestrationTaskService taskService;
    private final OrchestrationGraphService graphService;
    private final ObjectMapper objectMapper;

    public PlannerImportService(
            PlannerAuthorizationService authorizationService,
            PlannerPlanValidator planValidator,
            PlannerService plannerService,
            OrchestrationRunService runService,
            OrchestrationTaskService taskService,
            OrchestrationGraphService graphService,
            ObjectMapper objectMapper) {
        this.authorizationService = authorizationService;
        this.planValidator = planValidator;
        this.plannerService = plannerService;
        this.runService = runService;
        this.taskService = taskService;
        this.graphService = graphService;
        this.objectMapper = objectMapper;
    }

    public PlanAndCreateResponse planAndCreate(PlannerRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PlannerAuthorizationService.PLANNER_PLAN);
        authorizationService.require(user, PlannerAuthorizationService.PLANNER_IMPORT);
        // Plan (external AI) must not run inside a DB transaction.
        PlannerResponse planner = plannerService.plan(request, user);
        String runName = request.runName() == null || request.runName().isBlank()
                ? truncateName(planner.plan().objective())
                : request.runName().trim();
        RunResponse draft = importPlan(
                new ImportPlanRequest(request.projectId(), runName, planner.plan()), user);
        return new PlanAndCreateResponse(planner, draft);
    }

    @Transactional
    public RunResponse importPlan(ImportPlanRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PlannerAuthorizationService.PLANNER_IMPORT);
        ExecutionPlan plan = request.plan();
        planValidator.validate(plan);

        String metadataJson;
        try {
            metadataJson = objectMapper.writeValueAsString(Map.of(
                    "source", "planner",
                    "estimatedComplexity", plan.estimatedComplexity(),
                    "estimatedTokens", plan.estimatedTokens(),
                    "estimatedDurationSeconds", plan.estimatedDurationSeconds(),
                    "estimatedCostUsd", plan.estimatedCostUsd(),
                    "riskLevel", plan.riskLevel(),
                    "planMetadata", plan.metadata() == null ? Map.of() : plan.metadata()));
        } catch (Exception ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PLANNER_METADATA_INVALID", "Unable to serialize plan metadata");
        }

        int maxParallel = plan.maxParallelTasks() == null ? 1 : plan.maxParallelTasks();
        long maxDuration = plan.maximumDurationMs() == null
                ? Math.max(60_000L, (plan.estimatedDurationSeconds() == null ? 300L : plan.estimatedDurationSeconds()) * 1000L)
                : plan.maximumDurationMs();

        RunResponse run = runService.create(
                new CreateRunRequest(
                        request.projectId(),
                        request.runName().trim(),
                        plan.objective(),
                        plan.executionMode(),
                        plan.failurePolicy(),
                        maxParallel,
                        maxDuration,
                        null,
                        null,
                        metadataJson),
                user);

        Map<String, UUID> taskIdsByKey = new HashMap<>();
        List<ExecutionTaskDefinition> tasks = plan.tasks() == null ? List.of() : plan.tasks();
        int index = 1;
        for (ExecutionTaskDefinition task : tasks) {
            Integer sequence = task.sequenceOrder() != null ? task.sequenceOrder() : index;
            String description = appendRole(task.description(), task.agentRole(), task.classification());
            var created = taskService.create(
                    run.id(),
                    new CreateTaskRequest(
                            task.taskKey(),
                            task.displayName(),
                            description,
                            task.taskType(),
                            task.assignedAgentId(),
                            task.modelReference(),
                            null,
                            task.inputJson(),
                            1,
                            1000L,
                            task.priority() == null ? index : task.priority(),
                            120,
                            sequence,
                            task.taskKey()),
                    user);
            taskIdsByKey.put(task.taskKey(), created.id());
            index++;
        }

        List<ExecutionDependency> deps = plan.dependencies() == null ? List.of() : plan.dependencies();
        for (ExecutionDependency dep : deps) {
            UUID fromId = taskIdsByKey.get(dep.from());
            UUID toId = taskIdsByKey.get(dep.to());
            if (fromId == null || toId == null) {
                throw new ApiException(HttpStatus.BAD_REQUEST, "PLANNER_UNKNOWN_DEPENDENCY", "Dependency task missing after import");
            }
            DependencyType type = dep.type() == null ? DependencyType.SUCCESS : dep.type();
            graphService.addDependency(run.id(), new CreateDependencyRequest(fromId, toId, type), user);
        }

        // Reload summary after tasks/deps
        return runService.get(run.id(), user);
    }

    private static String appendRole(String description, String agentRole, Object classification) {
        String base = description == null ? "" : description.trim();
        String suffix = "agentRole=" + agentRole + (classification == null ? "" : "; classification=" + classification);
        if (base.isBlank()) {
            return suffix;
        }
        return base + " [" + suffix + "]";
    }

    private static String truncateName(String objective) {
        String name = "Plan: " + objective.trim();
        return name.length() <= 255 ? name : name.substring(0, 255);
    }
}
