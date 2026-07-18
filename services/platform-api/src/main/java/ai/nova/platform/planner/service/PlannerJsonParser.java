package ai.nova.platform.planner.service;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionDependency;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionPlan;
import ai.nova.platform.planner.dto.PlannerDtos.ExecutionTaskDefinition;
import ai.nova.platform.planner.entity.PlannerComplexity;
import ai.nova.platform.planner.entity.PlannerTaskClassification;
import ai.nova.platform.web.error.ApiException;

@Service
public class PlannerJsonParser {

    private final ObjectMapper objectMapper;

    public PlannerJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ExecutionPlan parse(String rawText, String fallbackObjective) {
        String json = extractJson(rawText);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw invalid("Planner output must be a JSON object");
            }
            String objective = text(root, "objective");
            if (objective == null || objective.isBlank()) {
                objective = fallbackObjective;
            }
            ExecutionMode mode = enumValue(root, "executionMode", ExecutionMode.class, ExecutionMode.DEPENDENCY_GRAPH);
            FailurePolicy policy = enumValue(root, "failurePolicy", FailurePolicy.class, FailurePolicy.FAIL_FAST);
            Integer maxParallel = root.path("maxParallelTasks").isNumber() ? root.path("maxParallelTasks").asInt() : 1;
            Long maxDuration = root.path("maximumDurationMs").isNumber()
                    ? root.path("maximumDurationMs").asLong()
                    : null;
            PlannerComplexity complexity =
                    enumValue(root, "estimatedComplexity", PlannerComplexity.class, null);
            Long tokens = root.path("estimatedTokens").isNumber() ? root.path("estimatedTokens").asLong() : null;
            Long duration =
                    root.path("estimatedDurationSeconds").isNumber()
                            ? root.path("estimatedDurationSeconds").asLong()
                            : null;
            Double cost =
                    root.path("estimatedCostUsd").isNumber() ? root.path("estimatedCostUsd").asDouble() : null;

            List<ExecutionTaskDefinition> tasks = new ArrayList<>();
            JsonNode tasksNode = root.path("tasks");
            if (tasksNode.isArray()) {
                for (JsonNode taskNode : tasksNode) {
                    tasks.add(parseTask(taskNode));
                }
            }

            List<ExecutionDependency> deps = new ArrayList<>();
            JsonNode depsNode = root.path("dependencies");
            if (depsNode.isArray()) {
                for (JsonNode depNode : depsNode) {
                    deps.add(parseDependency(depNode));
                }
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> metadata = root.has("metadata") && root.get("metadata").isObject()
                    ? objectMapper.convertValue(root.get("metadata"), Map.class)
                    : Map.of();

            return new ExecutionPlan(
                    objective,
                    mode,
                    policy,
                    maxParallel,
                    maxDuration,
                    complexity,
                    tokens,
                    duration,
                    cost,
                    null,
                    List.copyOf(tasks),
                    List.copyOf(deps),
                    metadata);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("Failed to parse planner JSON: " + ex.getMessage());
        }
    }

    private ExecutionTaskDefinition parseTask(JsonNode node) {
        String taskKey = text(node, "taskKey");
        String displayName = text(node, "displayName");
        String description = text(node, "description");
        TaskType taskType = enumValue(node, "taskType", TaskType.class, TaskType.AGENT_TURN);
        String agentRole = text(node, "agentRole");
        if (agentRole == null || agentRole.isBlank()) {
            agentRole = defaultRoleFor(taskType, text(node, "classification"));
        }
        PlannerTaskClassification classification =
                enumValue(node, "classification", PlannerTaskClassification.class, null);
        Integer priority = node.path("priority").isNumber() ? node.path("priority").asInt() : 100;
        Integer sequenceOrder =
                node.path("sequenceOrder").isNumber() ? node.path("sequenceOrder").asInt() : null;
        String modelReference = text(node, "modelReference");
        String inputJson = node.has("input") ? node.get("input").toString() : text(node, "inputJson");
        return new ExecutionTaskDefinition(
                taskKey,
                displayName == null ? taskKey : displayName,
                description,
                taskType,
                agentRole,
                classification,
                priority,
                sequenceOrder,
                null,
                modelReference,
                inputJson);
    }

    private ExecutionDependency parseDependency(JsonNode node) {
        String from = text(node, "from");
        if (from == null) {
            from = text(node, "predecessor");
        }
        String to = text(node, "to");
        if (to == null) {
            to = text(node, "successor");
        }
        DependencyType type = enumValue(node, "type", DependencyType.class, DependencyType.SUCCESS);
        if (type == null && node.has("dependencyType")) {
            type = enumValue(node, "dependencyType", DependencyType.class, DependencyType.SUCCESS);
        }
        return new ExecutionDependency(from, to, type == null ? DependencyType.SUCCESS : type);
    }

    private static String defaultRoleFor(TaskType taskType, String classification) {
        if (taskType == TaskType.HUMAN_APPROVAL) {
            return "human";
        }
        if (taskType == TaskType.TRANSFORM) {
            return "transform";
        }
        if (taskType == TaskType.AGGREGATION) {
            return "aggregation";
        }
        if (classification != null && !classification.isBlank()) {
            return classification.trim().toLowerCase(Locale.ROOT);
        }
        return "coding";
    }

    static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw invalid("Planner returned empty output");
        }
        String trimmed = rawText.trim();
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                trimmed = trimmed.substring(firstNl + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw invalid("Planner output did not contain a JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static <E extends Enum<E>> E enumValue(JsonNode node, String field, Class<E> type, E defaultValue) {
        String raw = text(node, field);
        if (raw == null || raw.isBlank()) {
            return defaultValue;
        }
        try {
            return Enum.valueOf(type, raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            throw invalid("Invalid value for " + field + ": " + raw);
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "PLANNER_INVALID_OUTPUT", message);
    }
}
