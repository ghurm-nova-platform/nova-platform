package ai.nova.platform.modelgateway.gateway;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.agent.runtime.RuntimeKnowledgeContext;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeToolResultMessage;
import ai.nova.platform.agent.runtime.RuntimeToolSpec;

public record ModelGatewayRequest(
        UUID organizationId,
        UUID projectId,
        UUID agentId,
        UUID executionId,
        UUID conversationId,
        UUID createdBy,
        String systemPrompt,
        List<RuntimeMessage> messages,
        List<RuntimeToolSpec> availableTools,
        List<RuntimeToolResultMessage> toolResults,
        RuntimeKnowledgeContext knowledgeContext,
        boolean requiresTools,
        boolean requiresKnowledge,
        String modelReference) {

    public ModelGatewayRequest(
            UUID organizationId,
            UUID projectId,
            UUID agentId,
            UUID executionId,
            UUID conversationId,
            UUID createdBy,
            String systemPrompt,
            List<RuntimeMessage> messages,
            List<RuntimeToolSpec> availableTools,
            List<RuntimeToolResultMessage> toolResults,
            RuntimeKnowledgeContext knowledgeContext,
            boolean requiresTools,
            boolean requiresKnowledge) {
        this(
                organizationId,
                projectId,
                agentId,
                executionId,
                conversationId,
                createdBy,
                systemPrompt,
                messages,
                availableTools,
                toolResults,
                knowledgeContext,
                requiresTools,
                requiresKnowledge,
                null);
    }
}
