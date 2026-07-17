package ai.nova.platform.agent.runtime;

import java.util.UUID;

/**
 * Server-to-server boundary for synchronizing agent definitions with Agent Runtime.
 * Browser clients must never call Agent Runtime.
 */
public interface AgentRuntimeClient {

    void createOrUpdateAgentDefinition(UUID organizationId, UUID projectId, UUID agentId, String name, String status);

    void archiveAgentDefinition(UUID organizationId, UUID projectId, UUID agentId);

    RuntimeTurnResult execute(ExecutionRequest request);

    void cancel(UUID executionId);
}
