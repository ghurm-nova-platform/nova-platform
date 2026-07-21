package ai.nova.platform.llm.provider;

import ai.nova.platform.llm.entity.LlmProviderType;

public record LlmCompletionResult(
        String content,
        int inputTokens,
        int outputTokens,
        long latencyMs,
        LlmProviderType providerType,
        String finishReason) {
}
