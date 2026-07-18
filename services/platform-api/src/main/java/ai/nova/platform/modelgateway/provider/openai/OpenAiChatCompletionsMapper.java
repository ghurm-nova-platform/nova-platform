package ai.nova.platform.modelgateway.provider.openai;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeToolCallRequest;
import ai.nova.platform.agent.runtime.RuntimeToolResultMessage;
import ai.nova.platform.agent.runtime.RuntimeToolSpec;
import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.provider.ProviderInvokeRequest;

public final class OpenAiChatCompletionsMapper {

    private final ObjectMapper objectMapper;
    private final ModelGatewayProperties properties;

    public OpenAiChatCompletionsMapper(ObjectMapper objectMapper, ModelGatewayProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
    }

    public ObjectNode toRequestBody(ProviderInvokeRequest request, String modelOrDeployment) {
        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", modelOrDeployment);
        body.put("max_tokens", Math.max(1, Math.min(request.maxOutputTokens(), properties.getMaximumOutputTokens())));

        ArrayNode messages = body.putArray("messages");
        if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
            ObjectNode system = messages.addObject();
            system.put("role", "system");
            system.put("content", boundText(request.systemPrompt(), properties.getMaxSystemCharacters()));
        }
        if (request.messages() != null) {
            for (RuntimeMessage message : request.messages()) {
                ObjectNode msg = messages.addObject();
                msg.put("role", toOpenAiRole(message.role()));
                msg.put("content", boundText(message.content(), properties.getMaxMessageCharacters()));
            }
        }
        if (request.toolResults() != null) {
            for (RuntimeToolResultMessage toolResult : request.toolResults()) {
                ObjectNode msg = messages.addObject();
                msg.put("role", "tool");
                msg.put("tool_call_id", toolResult.runtimeCallId());
                msg.put("name", toolResult.toolKey());
                String content = toolResult.output() != null ? toolResult.output().toString() : toolResult.errorCode();
                msg.put("content", boundText(content, properties.getMaxMessageCharacters()));
            }
        }
        if (request.availableTools() != null && !request.availableTools().isEmpty()) {
            ArrayNode tools = body.putArray("tools");
            for (RuntimeToolSpec tool : request.availableTools()) {
                ObjectNode toolNode = tools.addObject();
                toolNode.put("type", "function");
                ObjectNode function = toolNode.putObject("function");
                function.put("name", tool.toolKey());
                if (tool.description() != null) {
                    function.put("description", tool.description());
                }
                if (tool.inputSchema() != null) {
                    function.set("parameters", tool.inputSchema());
                } else {
                    function.putObject("parameters").put("type", "object");
                }
            }
        }
        return body;
    }

    public MappedCompletion mapResponse(JsonNode root) throws Exception {
        String finishReason = null;
        String content = null;
        List<RuntimeToolCallRequest> toolCalls = new ArrayList<>();
        JsonNode choices = root.path("choices");
        if (choices.isArray() && !choices.isEmpty()) {
            JsonNode choice = choices.get(0);
            finishReason = textOrNull(choice.path("finish_reason"));
            JsonNode message = choice.path("message");
            content = textOrNull(message.path("content"));
            JsonNode toolCallsNode = message.path("tool_calls");
            if (toolCallsNode.isArray()) {
                for (JsonNode call : toolCallsNode) {
                    String id = textOrNull(call.path("id"));
                    JsonNode function = call.path("function");
                    String name = textOrNull(function.path("name"));
                    String argsRaw = textOrNull(function.path("arguments"));
                    JsonNode args = argsRaw == null || argsRaw.isBlank()
                            ? objectMapper.createObjectNode()
                            : objectMapper.readTree(argsRaw);
                    if (id != null && name != null) {
                        toolCalls.add(new RuntimeToolCallRequest(id, name, args));
                    }
                }
            }
        }
        int inputTokens = root.path("usage").path("prompt_tokens").asInt(0);
        int outputTokens = root.path("usage").path("completion_tokens").asInt(0);
        String requestId = textOrNull(root.path("id"));
        if (content != null) {
            content = boundText(content, properties.getMaxOutputCharacters());
        }
        return new MappedCompletion(content, toolCalls, inputTokens, outputTokens, finishReason, requestId);
    }

    private static String toOpenAiRole(String role) {
        if (role == null) {
            return "user";
        }
        return switch (role.toUpperCase()) {
            case "SYSTEM" -> "system";
            case "ASSISTANT", "MODEL" -> "assistant";
            case "TOOL" -> "tool";
            default -> "user";
        };
    }

    private static String boundText(String value, int max) {
        if (value == null) {
            return "";
        }
        if (value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private static String textOrNull(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        String text = node.asText();
        return text == null || text.isBlank() ? null : text;
    }

    public record MappedCompletion(
            String content,
            List<RuntimeToolCallRequest> toolCalls,
            int inputTokens,
            int outputTokens,
            String finishReason,
            String providerRequestId) {
    }
}
