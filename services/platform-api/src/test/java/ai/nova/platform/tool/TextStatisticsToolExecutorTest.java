package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.tool.executor.TextStatisticsToolExecutor;
import ai.nova.platform.tool.executor.ToolExecutionContext;
import ai.nova.platform.tool.executor.ToolExecutionOutcome;
import ai.nova.platform.tool.validation.ToolInputValidator;

class TextStatisticsToolExecutorTest {

    private TextStatisticsToolExecutor executor;
    private ObjectMapper objectMapper;
    private ToolExecutionContext context;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = new TextStatisticsToolExecutor(objectMapper, new ToolInputValidator());
        context = new ToolExecutionContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "TEXT_STATISTICS", "TEXT_STATISTICS", 5000, 5);
    }

    @Test
    void computesStatistics() throws Exception {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("text", "hello world\nline two");

        var result = executor.execute(context, input);

        assertThat(result.status()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        assertThat(result.output().get("characters").asInt()).isEqualTo(20);
        assertThat(result.output().get("charactersWithoutSpaces").asInt()).isEqualTo(17);
        assertThat(result.output().get("words").asInt()).isEqualTo(4);
        assertThat(result.output().get("lines").asInt()).isEqualTo(2);
    }

    @Test
    void handlesBlankText() throws Exception {
        ObjectNode input = objectMapper.createObjectNode();
        input.put("text", "   ");

        var result = executor.execute(context, input);

        assertThat(result.status()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        assertThat(result.output().get("words").asInt()).isZero();
    }
}
