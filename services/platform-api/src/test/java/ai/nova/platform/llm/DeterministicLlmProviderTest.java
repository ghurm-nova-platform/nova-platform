package ai.nova.platform.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.provider.LlmCompletionRequest;
import ai.nova.platform.llm.provider.LlmCompletionRequest.LlmChatMessage;
import ai.nova.platform.llm.provider.LlmCompletionResult;
import ai.nova.platform.llm.provider.deterministic.DeterministicLlmProvider;

class DeterministicLlmProviderTest {

    private final DeterministicLlmProvider provider = new DeterministicLlmProvider();

    @Test
    void completePrefixesLastUserMessage() {
        LlmCompletionResult result = provider.complete(new LlmCompletionRequest(
                "deterministic-chat-v1",
                List.of(
                        new LlmChatMessage("system", "You are helpful"),
                        new LlmChatMessage("user", "hello world")),
                64,
                0.0,
                false));

        assertThat(result.content()).isEqualTo("Local LLM: hello world");
        assertThat(result.providerType()).isEqualTo(LlmProviderType.DETERMINISTIC);
        assertThat(result.inputTokens()).isGreaterThan(0);
        assertThat(result.outputTokens()).isGreaterThan(0);
    }
}
