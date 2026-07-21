package ai.nova.platform.llm.provider.llamacpp;

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
public class LlamaCppLlmProvider implements LlmRuntimeProvider {

    private final LlmProperties properties;
    private final OpenAiCompatibleClient client;

    public LlamaCppLlmProvider(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = new OpenAiCompatibleClient(objectMapper);
    }

    @Override
    public LlmProviderType providerType() {
        return LlmProviderType.LLAMA_CPP;
    }

    @Override
    public LlmCompletionResult complete(LlmCompletionRequest request) {
        if (!properties.getLlamacpp().isEnabled()) {
            throw new LlmProviderException(LlmErrorCodes.PROVIDER_UNAVAILABLE, "llama.cpp provider disabled", false);
        }
        return client.chatCompletions(properties.getLlamacpp().getBaseUrl(), request, LlmProviderType.LLAMA_CPP);
    }

    @Override
    public ProviderHealth health() {
        if (!properties.getLlamacpp().isEnabled()) {
            return new ProviderHealth(LlmProviderHealthStatus.DISABLED, "disabled");
        }
        try {
            client.chatCompletions(
                    properties.getLlamacpp().getBaseUrl(),
                    new LlmCompletionRequest(
                            "health-check",
                            List.of(new LlmCompletionRequest.LlmChatMessage("user", "ping")),
                            1,
                            0.0,
                            false),
                    LlmProviderType.LLAMA_CPP);
            return new ProviderHealth(LlmProviderHealthStatus.HEALTHY, "reachable");
        } catch (Exception ex) {
            return new ProviderHealth(LlmProviderHealthStatus.UNAVAILABLE, ex.getMessage());
        }
    }
}
