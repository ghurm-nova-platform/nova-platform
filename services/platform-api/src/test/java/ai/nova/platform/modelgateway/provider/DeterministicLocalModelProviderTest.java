package ai.nova.platform.modelgateway.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.NoOpAgentRuntimeClient;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeToolSpec;

class DeterministicLocalModelProviderTest {

    private final DeterministicLocalModelProvider provider =
            new DeterministicLocalModelProvider(new ObjectMapper());

    @Test
    void emitsCalculatorToolCallForMarker() throws Exception {
        ProviderInvokeResult result = provider.invoke(new ProviderInvokeRequest(
                DeterministicLocalModelProvider.MODEL_ID,
                "system",
                List.of(new RuntimeMessage("USER", NoOpAgentRuntimeClient.MARKER_CALCULATOR)),
                List.of(new RuntimeToolSpec("CALCULATOR", "Calculator", "calc", null)),
                List.of(),
                null,
                100,
                30,
                null));
        assertThat(result.outcome()).isEqualTo(ProviderInvokeOutcome.TOOL_CALLS);
        assertThat(result.toolCalls()).hasSize(1);
    }

    @Test
    void returnsFinalResponseForNormalInput() throws Exception {
        ProviderInvokeResult result = provider.invoke(new ProviderInvokeRequest(
                DeterministicLocalModelProvider.MODEL_ID,
                "system",
                List.of(new RuntimeMessage("USER", "hello")),
                List.of(),
                List.of(),
                null,
                100,
                30,
                null));
        assertThat(result.outcome()).isEqualTo(ProviderInvokeOutcome.FINAL);
        assertThat(result.responseText()).contains("Deterministic runtime response");
    }
}
