package ai.nova.platform.llm;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.provider.LlmCompletionRequest;
import ai.nova.platform.llm.provider.LlmCompletionRequest.LlmChatMessage;
import ai.nova.platform.llm.provider.LlmCompletionResult;
import ai.nova.platform.llm.service.LLMGatewayService;
import ai.nova.platform.llm.support.LlmTestFixture;

@SpringBootTest
class LLMGatewayServiceTest {

    @Autowired
    private LLMGatewayService gatewayService;

    @Test
    void completesViaDeterministicProvider() {
        LlmCompletionResult result = gatewayService.completeByModelCode(
                LlmTestFixture.ORG_ID,
                "deterministic-chat-v1",
                new LlmCompletionRequest(
                        "deterministic-chat-v1",
                        List.of(new LlmChatMessage("user", "gateway ping")),
                        32,
                        0.0,
                        false));

        assertThat(result.content()).isEqualTo("Local LLM: gateway ping");
        assertThat(result.providerType()).isEqualTo(LlmProviderType.DETERMINISTIC);
    }
}
