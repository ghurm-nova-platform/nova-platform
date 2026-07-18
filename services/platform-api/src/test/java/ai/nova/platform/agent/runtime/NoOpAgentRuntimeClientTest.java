package ai.nova.platform.agent.runtime;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class NoOpAgentRuntimeClientTest {

    private final NoOpAgentRuntimeClient client = new NoOpAgentRuntimeClient(new ObjectMapper());

    @Test
    void executeReturnsDeterministicFakeResponseWithTokensAndLatency() {
        UUID agentId = UUID.fromString("66666666-6666-6666-6666-666666666601");
        UUID executionId = UUID.randomUUID();
        String systemPrompt = "Hello {{customer_name}} about {{topic}}";
        String userMessage = "I need help with my order please";
        List<RuntimeMessage> messages = List.of(new RuntimeMessage("USER", userMessage));

        RuntimeTurnResult result = client.execute(new ExecutionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                agentId,
                executionId,
                "OPENAI",
                "gpt-4.1-mini",
                systemPrompt,
                messages,
                null,
                List.of(),
                List.of(),
                null));

        assertThat(result.isFinal()).isTrue();
        RuntimeFinalResponse finalResponse = result.finalResponse();
        assertThat(finalResponse.responseText())
                .startsWith("NoOp runtime response for agent " + agentId + ":")
                .contains("I need help with my order please");
        assertThat(finalResponse.inputTokens()).isGreaterThan(0);
        assertThat(finalResponse.outputTokens()).isGreaterThan(0);
        assertThat(finalResponse.totalTokens())
                .isEqualTo(finalResponse.inputTokens() + finalResponse.outputTokens());
        assertThat(finalResponse.latencyMs()).isBetween(100L, 350L);
    }

    @Test
    void usesLastUserMessageFromHistory() {
        UUID agentId = UUID.randomUUID();
        List<RuntimeMessage> messages = List.of(
                new RuntimeMessage("USER", "first question"),
                new RuntimeMessage("ASSISTANT", "first answer"),
                new RuntimeMessage("USER", "follow up question"));

        RuntimeTurnResult result = client.execute(new ExecutionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                agentId,
                UUID.randomUUID(),
                "OPENAI",
                "gpt-4.1-mini",
                "system",
                messages,
                UUID.randomUUID(),
                List.of(),
                List.of(),
                null));

        assertThat(result.finalResponse().responseText()).contains("follow up question");
    }

    @Test
    void calculatorMarkerReturnsToolCallWhenToolAvailable() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        RuntimeToolSpec calculator = new RuntimeToolSpec(
                "CALCULATOR",
                "Calculator",
                "Adds numbers",
                mapper.readTree(
                        "{\"type\":\"object\",\"properties\":{\"operation\":{\"type\":\"string\"}},\"required\":[\"operation\"]}"));

        RuntimeTurnResult result = client.execute(new ExecutionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "OPENAI",
                "gpt-4.1-mini",
                "system",
                List.of(new RuntimeMessage("USER", NoOpAgentRuntimeClient.MARKER_CALCULATOR)),
                null,
                List.of(calculator),
                List.of(),
                null));

        assertThat(result.isToolCalls()).isTrue();
        assertThat(result.toolCallBatch().toolCalls()).hasSize(1);
        assertThat(result.toolCallBatch().toolCalls().getFirst().toolKey()).isEqualTo("CALCULATOR");
        assertThat(result.toolCallBatch().toolCalls().getFirst().runtimeCallId()).isEqualTo("noop-calc-1");
    }

    @Test
    void toolResultsReturnFinalSummaryWithoutPayloadContent() {
        ObjectMapper mapper = new ObjectMapper();
        RuntimeToolResultMessage toolResult = new RuntimeToolResultMessage(
                "call-1",
                "CALCULATOR",
                "COMPLETED",
                mapper.createObjectNode().put("result", 15),
                null);

        RuntimeTurnResult result = client.execute(new ExecutionRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "OPENAI",
                "gpt-4.1-mini",
                "system",
                List.of(new RuntimeMessage("USER", "done")),
                null,
                List.of(),
                List.of(toolResult),
                null));

        assertThat(result.isFinal()).isTrue();
        assertThat(result.finalResponse().responseText()).contains("1 tool result");
        assertThat(result.finalResponse().responseText()).doesNotContain("15");
    }
}
