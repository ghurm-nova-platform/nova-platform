package ai.nova.platform.orchestration.service;

import java.util.Iterator;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.orchestration.config.OrchestrationProperties;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.web.error.ApiException;

@Component
public class TaskInputResolver {

    private final ObjectMapper objectMapper;
    private final OrchestrationProperties properties;

    public TaskInputResolver(ObjectMapper objectMapper, OrchestrationProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public String resolve(String inputJson, Map<String, AgentOrchestrationTask> tasksByKey) {
        if (inputJson == null || inputJson.isBlank()) {
            return inputJson;
        }
        if (inputJson.length() > properties.getMaxJsonChars()) {
            throw error("TASK_INPUT_TOO_LARGE", "Task input exceeds maximum size");
        }
        try {
            JsonNode root = objectMapper.readTree(inputJson);
            JsonNode resolved = resolveNode(root, tasksByKey);
            String out = objectMapper.writeValueAsString(resolved);
            if (out.length() > properties.getMaxJsonChars()) {
                throw error("TASK_INPUT_TOO_LARGE", "Resolved task input exceeds maximum size");
            }
            return out;
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw error("TASK_INPUT_REFERENCE_INVALID", "Invalid task input JSON");
        }
    }

    private JsonNode resolveNode(JsonNode node, Map<String, AgentOrchestrationTask> tasksByKey) {
        if (node == null || node.isNull()) {
            return node;
        }
        if (node.isObject()) {
            if (node.has("fromTask") && node.has("path") && node.size() == 2) {
                return resolveReference(node, tasksByKey);
            }
            ObjectNode copy = objectMapper.createObjectNode();
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                copy.set(entry.getKey(), resolveNode(entry.getValue(), tasksByKey));
            }
            return copy;
        }
        if (node.isArray()) {
            var arr = objectMapper.createArrayNode();
            for (JsonNode child : node) {
                arr.add(resolveNode(child, tasksByKey));
            }
            return arr;
        }
        return node;
    }

    private JsonNode resolveReference(JsonNode ref, Map<String, AgentOrchestrationTask> tasksByKey) {
        String fromTask = ref.path("fromTask").asText(null);
        String path = ref.path("path").asText(null);
        if (fromTask == null || fromTask.isBlank() || path == null || path.isBlank()) {
            throw error("TASK_INPUT_REFERENCE_INVALID", "fromTask and path are required");
        }
        if (!path.startsWith("$.")) {
            throw error("TASK_INPUT_REFERENCE_INVALID", "Only shallow $.field paths are supported");
        }
        String field = path.substring(2);
        if (field.contains(".") || field.contains("[") || field.isBlank()) {
            throw error("TASK_INPUT_REFERENCE_INVALID", "Only shallow $.field paths are supported");
        }
        AgentOrchestrationTask dep = tasksByKey.get(fromTask);
        if (dep == null) {
            throw error("TASK_INPUT_DEPENDENCY_NOT_FOUND", "Referenced task not found: " + fromTask);
        }
        if (dep.getStatus() != TaskStatus.SUCCEEDED
                || dep.getOutputJson() == null
                || dep.getOutputJson().isBlank()) {
            throw error("TASK_INPUT_OUTPUT_UNAVAILABLE", "Referenced task output unavailable: " + fromTask);
        }
        try {
            JsonNode output = objectMapper.readTree(dep.getOutputJson());
            if (!output.has(field)) {
                throw error("TASK_INPUT_PATH_NOT_FOUND", "Path not found: " + path);
            }
            return output.get(field).deepCopy();
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw error("TASK_INPUT_REFERENCE_INVALID", "Failed to resolve task reference");
        }
    }

    private static ApiException error(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message);
    }
}
