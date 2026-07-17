package ai.nova.platform.agent.runtime;

import java.util.UUID;

public record ExecutionRequest(
        UUID organizationId,
        UUID projectId,
        UUID agentId,
        UUID executionId,
        String provider,
        String model,
        String systemPrompt,
        String userMessage,
        UUID conversationId) {
}
