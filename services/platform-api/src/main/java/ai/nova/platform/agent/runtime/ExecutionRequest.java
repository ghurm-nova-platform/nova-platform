package ai.nova.platform.agent.runtime;

import java.util.List;
import java.util.UUID;

public record ExecutionRequest(
        UUID organizationId,
        UUID projectId,
        UUID agentId,
        UUID executionId,
        String provider,
        String model,
        String systemPrompt,
        List<RuntimeMessage> messages,
        UUID conversationId) {
}
