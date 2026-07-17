package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.tool.executor.CurrentDateTimeToolExecutor;
import ai.nova.platform.tool.executor.ToolExecutionContext;
import ai.nova.platform.tool.executor.ToolExecutionOutcome;
import ai.nova.platform.tool.validation.ToolInputValidator;

class CurrentDateTimeToolExecutorTest {

    private CurrentDateTimeToolExecutor executor;
    private ObjectMapper objectMapper;
    private ToolExecutionContext context;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = new CurrentDateTimeToolExecutor(objectMapper, new ToolInputValidator());
        context = new ToolExecutionContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "CURRENT_DATETIME", "CURRENT_DATETIME", 5000, 5);
    }

    @Test
    void returnsIsoDateTimeForAllowedTimezone() throws Exception {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("timezone", "Asia/Riyadh");

        var result = executor.execute(context, input);

        assertThat(result.status()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        assertThat(result.output().get("timezone").asText()).isEqualTo("Asia/Riyadh");
        assertThat(result.output().get("isoDateTime").asText()).isNotBlank();
    }

    @Test
    void rejectsUnknownTimezone() throws Exception {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("timezone", "Europe/Paris");

        var result = executor.execute(context, input);

        assertThat(result.status()).isEqualTo(ToolExecutionOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo("TOOL_INPUT_INVALID");
    }

    @Test
    void supportsAllAllowlistedZones() throws Exception {
        for (String zone : CurrentDateTimeToolExecutor.ALLOWED_TIMEZONES) {
            ObjectNode input = objectMapper.createObjectNode();
            input.put("timezone", zone);
            var result = executor.execute(context, input);
            assertThat(result.status()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        }
    }
}
