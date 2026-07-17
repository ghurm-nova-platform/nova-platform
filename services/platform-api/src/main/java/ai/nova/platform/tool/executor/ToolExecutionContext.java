package ai.nova.platform.tool.executor;

import java.util.UUID;

public record ToolExecutionContext(
        UUID organizationId,
        UUID projectId,
        UUID agentId,
        UUID executionId,
        UUID toolId,
        String toolKey,
        String executorKey,
        int maxOutputCharacters,
        int maxExecutionSeconds) {
}
