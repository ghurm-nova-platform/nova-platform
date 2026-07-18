package ai.nova.platform.modelgateway.provider;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.agent.runtime.NoOpAgentRuntimeClient;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeCitation;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeContext;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeToolCallRequest;
import ai.nova.platform.agent.runtime.RuntimeToolResultMessage;
import ai.nova.platform.agent.runtime.RuntimeToolSpec;

@Component
public class DeterministicLocalModelProvider implements AiModelProvider {

    public static final String ADAPTER_KEY = "DETERMINISTIC_LOCAL";
    public static final String MODEL_ID = "deterministic-chat-v1";

    public static final String MARKER_FAIL_TRANSIENT = "DETERMINISTIC_FAIL:TRANSIENT";
    public static final String MARKER_FAIL_PERMANENT = "DETERMINISTIC_FAIL:PERMANENT";
    public static final String MARKER_TIMEOUT = "DETERMINISTIC_FAIL:TIMEOUT";

    private final ObjectMapper objectMapper;

    public DeterministicLocalModelProvider(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public String adapterKey() {
        return ADAPTER_KEY;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(true, true, true, true, false);
    }

    @Override
    public ProviderInvokeResult invoke(ProviderInvokeRequest request) throws ProviderException {
        if (!MODEL_ID.equals(request.providerModelId())) {
            throw new ProviderException(
                    "MODEL_NOT_SUPPORTED", ProviderFailureKind.PERMANENT, "Unsupported model");
        }

        long startNanos = System.nanoTime();
        simulateLatency(request);

        List<RuntimeToolResultMessage> toolResults = request.toolResults() != null
                ? request.toolResults()
                : List.of();
        if (!toolResults.isEmpty()) {
            int completedCount = (int) toolResults.stream()
                    .filter(result -> "COMPLETED".equals(result.status()))
                    .count();
            int failedCount = toolResults.size() - completedCount;
            String responseText = "Deterministic runtime: processed "
                    + toolResults.size()
                    + " tool result(s) ("
                    + completedCount
                    + " completed, "
                    + failedCount
                    + " failed)";
            return finalizeResponse(request, responseText, startNanos);
        }

        String lastUser = NoOpAgentRuntimeClient.lastUserMessage(request.messages());
        checkFailureMarkers(lastUser);

        List<RuntimeToolSpec> availableTools = request.availableTools() != null
                ? request.availableTools()
                : List.of();

        if (NoOpAgentRuntimeClient.MARKER_CALCULATOR.equals(lastUser) && hasToolKey(availableTools, "CALCULATOR")) {
            ObjectNode arguments = objectMapper.createObjectNode();
            arguments.put("operation", "ADD");
            arguments.put("left", 10);
            arguments.put("right", 5);
            return ProviderInvokeResult.toolCalls(List.of(new RuntimeToolCallRequest(
                    "det-calc-1", "CALCULATOR", arguments)));
        }

        if (NoOpAgentRuntimeClient.MARKER_APPROVAL.equals(lastUser)
                && hasToolKey(availableTools, "CURRENT_DATETIME")) {
            ObjectNode arguments = objectMapper.createObjectNode();
            arguments.put("timezone", "Asia/Riyadh");
            return ProviderInvokeResult.toolCalls(List.of(new RuntimeToolCallRequest(
                    "det-approval-1", "CURRENT_DATETIME", arguments)));
        }

        String userPreview = lastUser.length() > 80 ? lastUser.substring(0, 80) : lastUser;
        String responseText = "Deterministic runtime response: " + userPreview;

        RuntimeKnowledgeContext knowledgeContext = request.knowledgeContext();
        if (knowledgeContext != null && !knowledgeContext.isEmpty()) {
            RuntimeKnowledgeCitation first = knowledgeContext.citations().isEmpty()
                    ? null
                    : knowledgeContext.citations().getFirst();
            String firstLabel = first != null ? first.label() : "K1";
            responseText = responseText
                    + " knowledgeCitations="
                    + knowledgeContext.citations().size()
                    + " firstCitation="
                    + firstLabel;
        }

        return finalizeResponse(request, responseText, startNanos);
    }

    private ProviderInvokeResult finalizeResponse(
            ProviderInvokeRequest request, String responseText, long startNanos) {
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;
        int inputTokens = estimateInputTokens(request);
        int outputTokens = wordCount(responseText);
        return ProviderInvokeResult.finalResponse(
                responseText, inputTokens, outputTokens, latencyMs, "stop");
    }

    private void checkFailureMarkers(String lastUser) throws ProviderException {
        if (MARKER_FAIL_TRANSIENT.equals(lastUser)) {
            throw new ProviderException("PROVIDER_UNAVAILABLE", ProviderFailureKind.TRANSIENT, "Transient failure");
        }
        if (MARKER_FAIL_PERMANENT.equals(lastUser)) {
            throw new ProviderException("PROVIDER_ERROR", ProviderFailureKind.PERMANENT, "Permanent failure");
        }
        if (MARKER_TIMEOUT.equals(lastUser)) {
            throw new ProviderException("PROVIDER_TIMEOUT", ProviderFailureKind.TRANSIENT, "Timeout");
        }
    }

    private void simulateLatency(ProviderInvokeRequest request) throws ProviderException {
        if (MARKER_TIMEOUT.equals(NoOpAgentRuntimeClient.lastUserMessage(request.messages()))) {
            try {
                Thread.sleep(Math.max(1, (request.timeoutSeconds() != null ? request.timeoutSeconds() : 30)) * 1000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw new ProviderException("PROVIDER_CANCELLED", ProviderFailureKind.TRANSIENT, "Interrupted");
            }
            throw new ProviderException("PROVIDER_TIMEOUT", ProviderFailureKind.TRANSIENT, "Timeout");
        }
        try {
            Thread.sleep(50);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ProviderException("PROVIDER_CANCELLED", ProviderFailureKind.TRANSIENT, "Interrupted");
        }
    }

    private static boolean hasToolKey(List<RuntimeToolSpec> tools, String toolKey) {
        return tools.stream().anyMatch(tool -> toolKey.equals(tool.toolKey()));
    }

    private static int estimateInputTokens(ProviderInvokeRequest request) {
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
