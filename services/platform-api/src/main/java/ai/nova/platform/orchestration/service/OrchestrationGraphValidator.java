package ai.nova.platform.orchestration.service;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskDependency;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.modelcatalog.service.ModelReferenceResolver;
import ai.nova.platform.web.error.ApiException;

@Component
public class OrchestrationGraphValidator {

    private final OrchestrationProperties properties;
    private final ModelReferenceResolver modelReferenceResolver;

    public OrchestrationGraphValidator(
            OrchestrationProperties properties, ModelReferenceResolver modelReferenceResolver) {
        this.properties = properties;
        this.modelReferenceResolver = modelReferenceResolver;
    }

    public void validate(
            AgentOrchestrationRun run,
            List<AgentOrchestrationTask> tasks,
            List<AgentTaskDependency> dependencies) {
        if (tasks == null || tasks.isEmpty()) {
            throw error("ORCHESTRATION_EMPTY", "Orchestration run has no tasks");
        }

        Map<UUID, AgentOrchestrationTask> byId = new HashMap<>();
        Set<String> keys = new HashSet<>();
        for (AgentOrchestrationTask task : tasks) {
            if (!task.getRunId().equals(run.getId())) {
                throw error("ORCHESTRATION_CROSS_RUN_DEPENDENCY", "Task does not belong to run");
            }
            if (task.getStatus() != TaskStatus.DRAFT
                    && task.getStatus() != TaskStatus.BLOCKED
                    && task.getStatus() != TaskStatus.READY) {
                throw error("INVALID_STATE_TRANSITION", "Task is already in a runtime state: " + task.getTaskKey());
            }
            if (!keys.add(task.getTaskKey())) {
                throw error("ORCHESTRATION_EMPTY", "Duplicate task key: " + task.getTaskKey());
            }
            byId.put(task.getId(), task);

            if (task.getMaxAttempts() < 1 || task.getMaxAttempts() > properties.getMaximumTaskAttempts()) {
                throw error("INVALID_INPUT", "maxAttempts out of range for task " + task.getTaskKey());
            }
            if (task.getTimeoutSeconds() < 1
                    || task.getTimeoutSeconds() > properties.getMaximumTaskTimeoutSeconds()) {
                throw error("INVALID_INPUT", "timeoutSeconds out of range for task " + task.getTaskKey());
            }
            if (task.getTaskType() == TaskType.AGENT_TURN) {
                boolean hasAgent = task.getAssignedAgentId() != null;
                boolean hasModel = task.getModelReference() != null && !task.getModelReference().isBlank();
                if (!hasAgent && !hasModel) {
                    throw error("ORCHESTRATION_AGENT_REQUIRED", "AGENT_TURN requires agent or modelReference: " + task.getTaskKey());
                }
                if (hasModel) {
                    try {
                        modelReferenceResolver.resolve(run.getOrganizationId(), task.getModelReference());
                    } catch (ApiException ex) {
                        throw error("ORCHESTRATION_MODEL_REFERENCE_INVALID", ex.getMessage());
                    }
                }
            }
            if (task.getRequiredCapabilitiesJson() != null && !task.getRequiredCapabilitiesJson().isBlank()) {
                String caps = task.getRequiredCapabilitiesJson().trim();
                if (!(caps.startsWith("[") && caps.endsWith("]"))) {
                    throw error("ORCHESTRATION_CAPABILITY_INVALID", "requiredCapabilities must be a JSON array");
                }
            }
        }

        Map<UUID, List<UUID>> adjacency = new HashMap<>();
        Map<UUID, Integer> indegree = new HashMap<>();
        for (UUID id : byId.keySet()) {
            adjacency.put(id, new ArrayList<>());
            indegree.put(id, 0);
        }

        for (AgentTaskDependency dep : dependencies) {
            if (!dep.getRunId().equals(run.getId())) {
                throw error("ORCHESTRATION_CROSS_RUN_DEPENDENCY", "Dependency run mismatch");
            }
            if (dep.getPredecessorTaskId().equals(dep.getSuccessorTaskId())) {
                throw error("ORCHESTRATION_SELF_DEPENDENCY", "Task cannot depend on itself");
            }
            if (!byId.containsKey(dep.getPredecessorTaskId()) || !byId.containsKey(dep.getSuccessorTaskId())) {
                throw error("ORCHESTRATION_CROSS_RUN_DEPENDENCY", "Dependency references unknown task");
            }
            adjacency.get(dep.getPredecessorTaskId()).add(dep.getSuccessorTaskId());
            indegree.merge(dep.getSuccessorTaskId(), 1, Integer::sum);
        }

        detectCycles(byId.keySet(), adjacency);

        List<UUID> roots = indegree.entrySet().stream()
                .filter(e -> e.getValue() == 0)
                .map(Map.Entry::getKey)
                .toList();
        if (roots.isEmpty()) {
            throw error("ORCHESTRATION_GRAPH_CYCLE", "Graph has no root tasks");
        }

        Set<UUID> reachable = new HashSet<>();
        ArrayDeque<UUID> queue = new ArrayDeque<>(roots);
        while (!queue.isEmpty()) {
            UUID id = queue.removeFirst();
            if (!reachable.add(id)) {
                continue;
            }
            for (UUID next : adjacency.getOrDefault(id, List.of())) {
                queue.add(next);
            }
        }
        if (reachable.size() != byId.size()) {
            throw error("ORCHESTRATION_TASK_UNREACHABLE", "Some tasks are unreachable from root tasks");
        }

        if (run.getExecutionMode() == ExecutionMode.SEQUENTIAL) {
            validateSequential(tasks);
        }

        if (run.getMaxParallelTasks() < 1 || run.getMaxParallelTasks() > properties.getMaximumParallelTasks()) {
            throw error("INVALID_INPUT", "maxParallelTasks out of range");
        }
    }

    private void validateSequential(List<AgentOrchestrationTask> tasks) {
        Set<Integer> orders = new HashSet<>();
        for (AgentOrchestrationTask task : tasks) {
            if (task.getSequenceOrder() == null) {
                throw error("ORCHESTRATION_SEQUENCE_INVALID", "SEQUENTIAL mode requires sequenceOrder on all tasks");
            }
            if (!orders.add(task.getSequenceOrder())) {
                throw error("ORCHESTRATION_SEQUENCE_INVALID", "Duplicate sequenceOrder: " + task.getSequenceOrder());
            }
        }
    }

    private void detectCycles(Set<UUID> nodes, Map<UUID, List<UUID>> adjacency) {
        Set<UUID> visiting = new HashSet<>();
        Set<UUID> visited = new HashSet<>();
        for (UUID node : nodes) {
            if (dfsCycle(node, adjacency, visiting, visited)) {
                throw error("ORCHESTRATION_GRAPH_CYCLE", "Dependency graph contains a cycle");
            }
        }
    }

    private boolean dfsCycle(
            UUID node, Map<UUID, List<UUID>> adjacency, Set<UUID> visiting, Set<UUID> visited) {
        if (visited.contains(node)) {
            return false;
        }
        if (!visiting.add(node)) {
            return true;
        }
        for (UUID next : adjacency.getOrDefault(node, List.of())) {
            if (dfsCycle(next, adjacency, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }

    private static ApiException error(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
