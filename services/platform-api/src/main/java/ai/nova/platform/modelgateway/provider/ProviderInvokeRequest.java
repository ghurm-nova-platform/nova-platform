package ai.nova.platform.modelgateway.provider;

import java.util.List;

import ai.nova.platform.agent.runtime.RuntimeKnowledgeContext;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeToolResultMessage;
import ai.nova.platform.agent.runtime.RuntimeToolSpec;

public record ProviderInvokeRequest(
        String providerModelId,
        String systemPrompt,
        List<RuntimeMessage> messages,
        List<RuntimeToolSpec> availableTools,
        List<RuntimeToolResultMessage> toolResults,
        RuntimeKnowledgeContext knowledgeContext,
        int maxOutputTokens,
        Integer timeoutSeconds,
        String credentialSecret,
        ProviderEndpointConfig endpoint) {

    public ProviderInvokeRequest(
            String providerModelId,
            String systemPrompt,
            List<RuntimeMessage> messages,
            List<RuntimeToolSpec> availableTools,
            List<RuntimeToolResultMessage> toolResults,
            RuntimeKnowledgeContext knowledgeContext,
            int maxOutputTokens,
            Integer timeoutSeconds,
            String credentialSecret) {
        this(
                providerModelId,
                systemPrompt,
                messages,
                availableTools,
                toolResults,
                knowledgeContext,
                maxOutputTokens,
                timeoutSeconds,
                credentialSecret,
                null);
    }
}
