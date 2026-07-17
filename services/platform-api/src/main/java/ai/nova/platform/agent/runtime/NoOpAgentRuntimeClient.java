package ai.nova.platform.agent.runtime;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * No-op Agent Runtime client used until runtime sync is enabled.
 * Database transactions intentionally do not depend on this client.
 */
@Component
@ConditionalOnProperty(name = "nova.agent-runtime.enabled", havingValue = "false", matchIfMissing = true)
public class NoOpAgentRuntimeClient implements AgentRuntimeClient {

    private static final Logger log = LoggerFactory.getLogger(NoOpAgentRuntimeClient.class);

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
    public ExecutionResult execute(ExecutionRequest request) {
        long startNanos = System.nanoTime();
        try {
            Thread.sleep(100 + (Math.abs(request.executionId().hashCode()) % 201));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Execution interrupted", ex);
        }
        long latencyMs = (System.nanoTime() - startNanos) / 1_000_000L;

        String userPreview = lastUserMessage(request.messages());
        if (userPreview.length() > 80) {
            userPreview = userPreview.substring(0, 80);
        }
        String responseText =
                "NoOp runtime response for agent " + request.agentId() + ": " + userPreview;

        int inputTokens = wordCount(request.systemPrompt()) + wordCount(userPreview);
        for (RuntimeMessage message : request.messages()) {
            inputTokens += wordCount(message.content());
        }
        int outputTokens = wordCount(responseText);
        return new ExecutionResult(responseText, inputTokens, outputTokens, inputTokens + outputTokens, latencyMs);
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

    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
