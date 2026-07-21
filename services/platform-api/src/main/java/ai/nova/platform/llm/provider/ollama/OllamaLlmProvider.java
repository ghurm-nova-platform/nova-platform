package ai.nova.platform.llm.provider.ollama;

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
public class OllamaLlmProvider implements LlmRuntimeProvider {

    private final LlmProperties properties;
    private final OpenAiCompatibleClient client;

    public OllamaLlmProvider(LlmProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.client = new OpenAiCompatibleClient(objectMapper);
    }

    @Override
    public LlmProviderType providerType() {
        return LlmProviderType.OLLAMA;
    }

    @Override
    public LlmCompletionResult complete(LlmCompletionRequest request) {
        if (!properties.getOllama().isEnabled()) {
            throw new LlmProviderException(LlmErrorCodes.PROVIDER_UNAVAILABLE, "Ollama provider disabled", false);
        }
        return client.ollamaChat(properties.getOllama().getBaseUrl(), request);
    }

    @Override
    public ProviderHealth health() {
        if (!properties.getOllama().isEnabled()) {
            return new ProviderHealth(LlmProviderHealthStatus.DISABLED, "disabled");
        }
        try {
            client.ollamaChat(
                    properties.getOllama().getBaseUrl(),
                    new LlmCompletionRequest(
                            "health-check",
                            List.of(new LlmCompletionRequest.LlmChatMessage("user", "ping")),
                            1,
                            0.0,
                            false));
            return new ProviderHealth(LlmProviderHealthStatus.HEALTHY, "reachable");
        } catch (Exception ex) {
            return new ProviderHealth(LlmProviderHealthStatus.UNAVAILABLE, ex.getMessage());
        }
    }
}
