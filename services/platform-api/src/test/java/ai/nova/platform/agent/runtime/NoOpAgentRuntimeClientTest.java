package ai.nova.platform.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class NoOpAgentRuntimeClientTest {

    private final NoOpAgentRuntimeClient client = new NoOpAgentRuntimeClient();

    @Test
    void executeReturnsDeterministicFakeResponseWithTokensAndLatency() {
        UUID agentId = UUID.fromString("66666666-6666-6666-6666-666666666601");
        UUID executionId = UUID.randomUUID();
        String systemPrompt = "Hello {{customer_name}} about {{topic}}";
        String userMessage = "I need help with my order please";

        ExecutionResult result = client.execute(new ExecutionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                agentId,
                executionId,
                "OPENAI",
                "gpt-4.1-mini",
                systemPrompt,
                userMessage,
                null));

        assertThat(result.responseText())
                .startsWith("NoOp runtime response for agent " + agentId + ":")
                .contains("I need help with my order please");
        assertThat(result.inputTokens()).isGreaterThan(0);
        assertThat(result.outputTokens()).isGreaterThan(0);
        assertThat(result.totalTokens()).isEqualTo(result.inputTokens() + result.outputTokens());
        assertThat(result.latencyMs()).isBetween(100L, 350L);
    }
}
