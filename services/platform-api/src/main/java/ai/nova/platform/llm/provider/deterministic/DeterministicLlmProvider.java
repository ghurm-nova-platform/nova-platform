package ai.nova.platform.llm.provider.deterministic;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.nova.platform.llm.entity.LlmProviderHealthStatus;
import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.provider.LlmCompletionRequest;
import ai.nova.platform.llm.provider.LlmCompletionRequest.LlmChatMessage;
import ai.nova.platform.llm.provider.LlmCompletionResult;
import ai.nova.platform.llm.provider.LlmRuntimeProvider;

@Component
public class DeterministicLlmProvider implements LlmRuntimeProvider {

    @Override
    public LlmProviderType providerType() {
        return LlmProviderType.DETERMINISTIC;
    }

    @Override
    public LlmCompletionResult complete(LlmCompletionRequest request) {
        long start = System.nanoTime();
        String lastUser = lastUserMessage(request.messages());
        String content = "Local LLM: " + lastUser;
        int inputTokens = estimateTokens(request.messages());
        int outputTokens = estimateTokens(content);
        long latencyMs = (System.nanoTime() - start) / 1_000_000L;
        return new LlmCompletionResult(
                content, inputTokens, outputTokens, latencyMs, LlmProviderType.DETERMINISTIC, "stop");
    }

    @Override
    public ProviderHealth health() {
        return new ProviderHealth(LlmProviderHealthStatus.HEALTHY, "deterministic always available");
    }

    private static String lastUserMessage(List<LlmChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        for (int i = messages.size() - 1; i >= 0; i--) {
            LlmChatMessage message = messages.get(i);
            if (message != null && "user".equalsIgnoreCase(message.role()) && message.content() != null) {
                return message.content();
            }
        }
        LlmChatMessage last = messages.get(messages.size() - 1);
        return last != null && last.content() != null ? last.content() : "";
    }

    static int estimateTokens(List<LlmChatMessage> messages) {
        if (messages == null) {
            return 0;
        }
        int total = 0;
        for (LlmChatMessage message : messages) {
            if (message != null && message.content() != null) {
                total += estimateTokens(message.content());
            }
        }
        return total;
    }

    static int estimateTokens(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return Math.max(1, text.trim().split("\\s+").length);
    }
}
