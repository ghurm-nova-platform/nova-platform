package ai.nova.platform.agent.runtime;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * No-op Agent Runtime client used until runtime sync is enabled.
 * Database transactions intentionally do not depend on this client.
 */
@Component
@ConditionalOnProperty(name = "nova.agent-runtime.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAgentRuntimeClient implements AgentRuntimeClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpAgentRuntimeClient.class);

    public static final String MARKER_CALCULATOR = "NOOP_TOOL:CALCULATOR";
    public static final String MARKER_APPROVAL = "NOOP_TOOL:APPROVAL";

    private final ObjectMapper objectMapper;

    public NoOpAgentRuntimeClient(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public void createOrUpdateAgentDefinition(
            UUID organizationId, UUID projectId, UUID agentId, String name, String status) {
        log.debug(
                "Skipping Agent Runtime sync for agent {} in project {} (runtime disabled)",
                agentId,
                projectId);
    }

    @Override
    public void archiveAgentDefinition(UUID organizationId, UUID projectId, UUID agentId) {
        log.debug(
                "Skipping Agent Runtime archive for agent {} in project {} (runtime disabled)",
                agentId,
                projectId);
    }

    @Override
    public RuntimeTurnResult execute(ExecutionRequest request) {
        long startNanos = System.nanoTime();
        try {
            Thread.sleep(100 + (Math.abs(request.executionId().hashCode()) % 201));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Execution interrupted", ex);
        }
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;

        List<RuntimeToolResultMessage> toolResults = request.toolResults() != null
                ? request.toolResults()
                : List.of();
        if (!toolResults.isEmpty()) {
            int completedCount = (int) toolResults.stream()
                    .filter(result -> "COMPLETED".equals(result.status()))
                    .count();
            int failedCount = toolResults.size() - completedCount;
            String responseText = "NoOp runtime: processed "
                    + toolResults.size()
                    + " tool result(s) ("
                    + completedCount
                    + " completed, "
                    + failedCount
                    + " failed)";
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(
                    responseText,
                    estimateInputTokens(request),
                    wordCount(responseText),
                    estimateInputTokens(request) + wordCount(responseText),
                    latencyMs));
        }

        String lastUser = lastUserMessage(request.messages());
        List<RuntimeToolSpec> availableTools = request.availableTools() != null
                ? request.availableTools()
                : List.of();

        if (MARKER_CALCULATOR.equals(lastUser) && hasToolKey(availableTools, "CALCULATOR")) {
            ObjectNode arguments = objectMapper.createObjectNode();
            arguments.put("operation", "ADD");
            arguments.put("left", 10);
            arguments.put("right", 5);
            RuntimeToolCallRequest toolCall = new RuntimeToolCallRequest(
                    "noop-calc-1", "CALCULATOR", arguments);
            return RuntimeTurnResult.toolCalls(new RuntimeToolCallBatch(List.of(toolCall)));
        }

        if (MARKER_APPROVAL.equals(lastUser) && hasToolKey(availableTools, "CURRENT_DATETIME")) {
            ObjectNode arguments = objectMapper.createObjectNode();
            arguments.put("timezone", "Asia/Riyadh");
            RuntimeToolCallRequest toolCall = new RuntimeToolCallRequest(
                    "noop-approval-1", "CURRENT_DATETIME", arguments);
            return RuntimeTurnResult.toolCalls(new RuntimeToolCallBatch(List.of(toolCall)));
        }

        String userPreview = lastUser;
        if (userPreview.length() > 80) {
            userPreview = userPreview.substring(0, 80);
        }
        String responseText =
                "NoOp runtime response for agent " + request.agentId() + ": " + userPreview;

        int inputTokens = estimateInputTokens(request);
        int outputTokens = wordCount(responseText);
        return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(
                responseText, inputTokens, outputTokens, inputTokens + outputTokens, latencyMs));
    }

    @Override
    public void cancel(UUID executionId) {
        log.debug("Skipping Agent Runtime cancel for execution {} (runtime disabled)", executionId);
    }

    static String lastUserMessage(List<RuntimeMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        String lastUser = "";
        for (RuntimeMessage message : messages) {
            if ("USER".equals(message.role())) {
                lastUser = message.content();
            }
        }
        return lastUser;
    }

    private static boolean hasToolKey(List<RuntimeToolSpec> tools, String toolKey) {
        return tools.stream().anyMatch(tool -> toolKey.equals(tool.toolKey()));
    }

    private static int estimateInputTokens(ExecutionRequest request) {
        int inputTokens = wordCount(request.systemPrompt());
        for (RuntimeMessage message : request.messages()) {
            inputTokens += wordCount(message.content());
        }
        return inputTokens;
    }

    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
