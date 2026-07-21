package ai.nova.platform.llm.provider.vllm;

import java.util.List;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.llm.configuration.LlmProperties;
import ai.nova.platform.llm.entity.LlmProviderHealthStatus;
import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.provider.LlmCompletionRequest;
import ai.nova.platform.llm.provider.LlmCompletionResult;
import ai.nova.platform.llm.provider.LlmProviderException;
import ai.nova.platform.llm.provider.LlmRuntimeProvider;
import ai.nova.platform.llm.provider.OpenAiCompatibleClient;

@Component
public class VllmLlmProvider implements LlmRuntimeProvider {

    private final LlmProperties properties;
    private final OpenAiCompatibleClient client;

    public VllmLlmProvider(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = new OpenAiCompatibleClient(objectMapper);
    }

    @Override
    public LlmProviderType providerType() {
        return LlmProviderType.VLLM;
    }

    @Override
    public LlmCompletionResult complete(LlmCompletionRequest request) {
        if (!properties.getVllm().isEnabled()) {
            throw new LlmProviderException(LlmErrorCodes.PROVIDER_UNAVAILABLE, "vLLM provider disabled", false);
        }
        return client.chatCompletions(properties.getVllm().getBaseUrl(), request, LlmProviderType.VLLM);
    }

    @Override
    public ProviderHealth health() {
        if (!properties.getVllm().isEnabled()) {
            return new ProviderHealth(LlmProviderHealthStatus.DISABLED, "disabled");
        }
        try {
            client.chatCompletions(
                    properties.getVllm().getBaseUrl(),
                    new LlmCompletionRequest(
                            "health-check",
                            List.of(new LlmCompletionRequest.LlmChatMessage("user", "ping")),
                            1,
                            0.0,
                            false),
                    LlmProviderType.VLLM);
            return new ProviderHealth(LlmProviderHealthStatus.HEALTHY, "reachable");
        } catch (Exception ex) {
            return new ProviderHealth(LlmProviderHealthStatus.UNAVAILABLE, ex.getMessage());
        }
    }
}
