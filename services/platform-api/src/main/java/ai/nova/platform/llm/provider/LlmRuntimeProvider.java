package ai.nova.platform.llm.provider;

import ai.nova.platform.llm.entity.LlmProviderHealthStatus;
import ai.nova.platform.llm.entity.LlmProviderType;

public interface LlmRuntimeProvider {

    LlmProviderType providerType();

    LlmCompletionResult complete(LlmCompletionRequest request);

    ProviderHealth health();

    record ProviderHealth(LlmProviderHealthStatus status, String detail) {
    }
}
