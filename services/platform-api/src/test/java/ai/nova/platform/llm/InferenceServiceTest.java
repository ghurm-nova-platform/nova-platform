package ai.nova.platform.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.llm.dto.LlmDtos.ChatCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.ChatMessageDto;
import ai.nova.platform.llm.dto.LlmDtos.CompletionResponse;
import ai.nova.platform.llm.dto.LlmDtos.TextCompletionRequest;
import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.service.InferenceService;
import ai.nova.platform.llm.support.LlmTestFixture;

@SpringBootTest
class InferenceServiceTest {

    @Autowired
    private InferenceService inferenceService;

    @Test
    void chatAndTextCompletionUseDeterministicModel() {
        CompletionResponse chat = inferenceService.chatCompletion(
                new ChatCompletionRequest(
                        "deterministic-chat-v1",
                        LlmTestFixture.SEED_MODEL_ID,
                        null,
                        List.of(new ChatMessageDto("user", "infer me")),
                        64,
                        0.1,
                        null,
                        null),
                LlmTestFixture.llmAdminUser());

        assertThat(chat.content()).isEqualTo("Local LLM: infer me");
        assertThat(chat.providerType()).isEqualTo(LlmProviderType.DETERMINISTIC);

        CompletionResponse text = inferenceService.textCompletion(
                new TextCompletionRequest("deterministic-chat-v1", null, "plain prompt", 32, 0.0),
                LlmTestFixture.llmAdminUser());
        assertThat(text.content()).isEqualTo("Local LLM: plain prompt");
    }
}
