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
import ai.nova.platform.planner.security.PlannerAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

/**
 * Atomic DRAFT orchestration import. Must be invoked through the Spring proxy
 * (never via self-invocation) so {@code @Transactional} applies.
 * Nested orchestration services use default REQUIRED propagation and join this transaction.
 */
@Service
public class PlannerPlanImporter {

    private final PlannerAuthorizationService authorizationService;
    private final PlannerPlanValidator planValidator;
    private final OrchestrationRunService runService;
    private final OrchestrationTaskService taskService;
    private final OrchestrationGraphService graphService;
    private final ObjectMapper objectMapper;

    public PlannerPlanImporter(
            PlannerAuthorizationService authorizationService,
            PlannerPlanValidator planValidator,
            OrchestrationRunService runService,
            OrchestrationTaskService taskService,
            OrchestrationGraphService graphService,
            ObjectMapper objectMapper) {
        this.authorizationService = authorizationService;
        this.planValidator = planValidator;
        this.runService = runService;
        this.taskService = taskService;
        this.graphService = graphService;
        this.objectMapper = objectMapper;
    }

    /**
     * Creates the DRAFT run, all tasks, and all dependencies in one transaction.
     * Any failure rolls back the run, tasks, dependencies, and related events.
     */
    @Transactional
    public RunResponse importPlan(ImportPlanRequest request, AuthenticatedUser user) {
        authorizationService.require(user, PlannerAuthorizationService.PLANNER_IMPORT);
        ExecutionPlan plan = request.plan();
        planValidator.validate(plan);

        String metadataJson;
        try {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", "planner");
            if (plan.estimatedComplexity() != null) {
                metadata.put("estimatedComplexity", plan.estimatedComplexity().name());
            }
            if (plan.estimatedTokens() != null) {
                metadata.put("estimatedTokens", plan.estimatedTokens());
            }
            if (plan.estimatedDurationSeconds() != null) {
                metadata.put("estimatedDurationSeconds", plan.estimatedDurationSeconds());
            }
            if (plan.estimatedCostUsd() != null) {
                metadata.put("estimatedCostUsd", plan.estimatedCostUsd());
            }
            if (plan.riskLevel() != null) {
                metadata.put("riskLevel", plan.riskLevel().name());
            }
            metadata.put("planMetadata", plan.metadata() == null ? Map.of() : plan.metadata());
            metadataJson = objectMapper.writeValueAsString(metadata);
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
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "PLANNER_UNKNOWN_DEPENDENCY", "Dependency task missing after import");
            }
            DependencyType type = dep.type() == null ? DependencyType.SUCCESS : dep.type();
            graphService.addDependency(run.id(), new CreateDependencyRequest(fromId, toId, type), user);
        }

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
}
