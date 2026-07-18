package ai.nova.platform.planner.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.planner.config.PlannerProperties;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionDependency;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionPlan;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionTaskDefinition;
import ai.nova.platform.web.error.ApiException;

@Service
public class PlannerPlanValidator {

    private static final Pattern TASK_KEY = Pattern.compile("^[a-z0-9][a-z0-9._:-]{0,149}$");

    private final PlannerPromptBuilder promptBuilder;
    private final PlannerProperties properties;

    public PlannerPlanValidator(PlannerPromptBuilder promptBuilder, PlannerProperties properties) {
        this.promptBuilder = promptBuilder;
        this.properties = properties;
    }

    public void validate(ExecutionPlan plan) {
        if (plan == null) {
            throw error("PLANNER_EMPTY_PLAN", "Execution plan is required");
        }
        if (plan.objective() == null || plan.objective().isBlank()) {
            throw error("PLANNER_OBJECTIVE_REQUIRED", "Objective is required");
        }
        if (plan.executionMode() == null) {
            throw error("PLANNER_EXECUTION_MODE_INVALID", "executionMode is required");
        }
        if (plan.failurePolicy() == null) {
            throw error("PLANNER_FAILURE_POLICY_INVALID", "failurePolicy is required");
        }
        try {
            ExecutionMode.valueOf(plan.executionMode().name());
        } catch (RuntimeException ex) {
            throw error("PLANNER_EXECUTION_MODE_INVALID", "Invalid executionMode");
        }
        try {
            FailurePolicy.valueOf(plan.failurePolicy().name());
        } catch (RuntimeException ex) {
            throw error("PLANNER_FAILURE_POLICY_INVALID", "Invalid failurePolicy");
        }

        List<ExecutionTaskDefinition> tasks = plan.tasks() == null ? List.of() : plan.tasks();
        if (tasks.isEmpty()) {
            throw error("PLANNER_EMPTY_TASKS", "Plan must include at least one task");
        }
        if (tasks.size() > properties.getMaxTasks()) {
            throw error("PLANNER_TOO_MANY_TASKS", "Task count exceeds platform maximum");
        }

        Set<String> keys = new HashSet<>();
        for (ExecutionTaskDefinition task : tasks) {
            if (task.taskKey() == null || !TASK_KEY.matcher(task.taskKey()).matches()) {
                throw error("PLANNER_TASK_KEY_INVALID", "Invalid taskKey: " + task.taskKey());
            }
            if (!keys.add(task.taskKey())) {
                throw error("PLANNER_DUPLICATE_TASK_KEY", "Duplicate taskKey: " + task.taskKey());
            }
            if (task.displayName() == null || task.displayName().isBlank()) {
                throw error("PLANNER_DISPLAY_NAME_REQUIRED", "displayName is required for " + task.taskKey());
            }
            if (task.taskType() == null) {
                throw error("PLANNER_TASK_TYPE_INVALID", "taskType is required for " + task.taskKey());
            }
            try {
                TaskType.valueOf(task.taskType().name());
            } catch (RuntimeException ex) {
                throw error("PLANNER_TASK_TYPE_INVALID", "Invalid taskType for " + task.taskKey());
            }
            if (task.agentRole() == null || task.agentRole().isBlank()) {
                throw error("PLANNER_AGENT_ROLE_REQUIRED", "agentRole is required for " + task.taskKey());
            }
            if (!promptBuilder.isAllowedRole(task.agentRole())) {
                throw error(
                        "PLANNER_AGENT_ROLE_INVALID",
                        "Unknown agentRole '" + task.agentRole() + "'. Allowed: " + promptBuilder.rolesCsv());
            }
        }

        if (plan.executionMode() == ExecutionMode.SEQUENTIAL) {
            Set<Integer> orders = new HashSet<>();
            for (ExecutionTaskDefinition task : tasks) {
                if (task.sequenceOrder() == null) {
                    throw error("PLANNER_SEQUENCE_INVALID", "sequenceOrder required in SEQUENTIAL mode");
                }
                if (!orders.add(task.sequenceOrder())) {
                    throw error("PLANNER_SEQUENCE_INVALID", "Duplicate sequenceOrder");
                }
            }
        }

        List<ExecutionDependency> deps = plan.dependencies() == null ? List.of() : plan.dependencies();
        if (deps.size() > properties.getMaxDependencies()) {
            throw error("PLANNER_TOO_MANY_DEPENDENCIES", "Dependency count exceeds platform maximum");
        }

        Map<String, List<String>> adjacency = new HashMap<>();
        for (String key : keys) {
            adjacency.put(key, new ArrayList<>());
        }
        for (ExecutionDependency dep : deps) {
            if (dep.from() == null || dep.to() == null || !keys.contains(dep.from()) || !keys.contains(dep.to())) {
                throw error("PLANNER_UNKNOWN_DEPENDENCY", "Dependency references unknown taskKey");
            }
            if (dep.from().equals(dep.to())) {
                throw error("PLANNER_SELF_DEPENDENCY", "Task cannot depend on itself");
            }
            if (dep.type() == null) {
                throw error("PLANNER_DEPENDENCY_TYPE_INVALID", "Dependency type is required");
            }
            try {
                DependencyType.valueOf(dep.type().name());
            } catch (RuntimeException ex) {
                throw error("PLANNER_DEPENDENCY_TYPE_INVALID", "Invalid dependency type");
            }
            adjacency.get(dep.from()).add(dep.to());
        }

        detectCycle(adjacency);
        ensureReachable(keys, deps);
    }

    private void detectCycle(Map<String, List<String>> adjacency) {
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (String node : adjacency.keySet()) {
            if (dfsCycle(node, adjacency, visiting, visited)) {
                throw error("PLANNER_GRAPH_CYCLE", "Execution plan contains a dependency cycle");
            }
        }
    }

    private boolean dfsCycle(
            String node, Map<String, List<String>> adjacency, Set<String> visiting, Set<String> visited) {
        if (visited.contains(node)) {
            return false;
        }
        if (!visiting.add(node)) {
            return true;
        }
        for (String next : adjacency.getOrDefault(node, List.of())) {
            if (dfsCycle(next, adjacency, visiting, visited)) {
                return true;
            }
        }
        visiting.remove(node);
        visited.add(node);
        return false;
    }

    private void ensureReachable(Set<String> keys, List<ExecutionDependency> deps) {
        Set<String> successors = new HashSet<>();
        Map<String, List<String>> forward = new HashMap<>();
        for (String key : keys) {
            forward.put(key, new ArrayList<>());
        }
        for (ExecutionDependency dep : deps) {
            successors.add(dep.to());
            forward.get(dep.from()).add(dep.to());
        }
        Set<String> roots = new HashSet<>(keys);
        roots.removeAll(successors);
        if (roots.isEmpty() && keys.size() > 1) {
            throw error("PLANNER_GRAPH_CYCLE", "Execution plan has no root tasks");
        }
        if (roots.isEmpty()) {
            roots.addAll(keys);
        }
        Set<String> reachable = new HashSet<>();
        for (String root : roots) {
            dfsReach(root, forward, reachable);
        }
        if (!reachable.containsAll(keys)) {
            throw error("PLANNER_TASK_UNREACHABLE", "One or more tasks are unreachable from root tasks");
        }
    }

    private void dfsReach(String node, Map<String, List<String>> forward, Set<String> reachable) {
        if (!reachable.add(node)) {
            return;
        }
        for (String next : forward.getOrDefault(node, List.of())) {
            dfsReach(next, forward, reachable);
        }
    }

    private static ApiException error(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
