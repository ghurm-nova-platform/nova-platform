package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.tool.executor.CalculatorToolExecutor;
import ai.nova.platform.tool.executor.ToolExecutionContext;
import ai.nova.platform.tool.executor.ToolExecutionOutcome;
import ai.nova.platform.tool.validation.ToolInputValidator;

class CalculatorToolExecutorTest {

    private CalculatorToolExecutor executor;
    private ObjectMapper objectMapper;
    private ToolExecutionContext context;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        executor = new CalculatorToolExecutor(objectMapper, new ToolInputValidator());
        context = new ToolExecutionContext(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "CALCULATOR", "CALCULATOR", 5000, 5);
    }

    @Test
    void addsNumbers() throws Exception {
        var result = executor.execute(context, input("ADD", 2, 3));
        assertThat(result.status()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        assertThat(result.output().get("result").decimalValue()).isEqualByComparingTo(BigDecimal.valueOf(5));
    }

    @Test
    void dividesNumbers() throws Exception {
        var result = executor.execute(context, input("DIVIDE", 10, 4));
        assertThat(result.status()).isEqualTo(ToolExecutionOutcome.SUCCESS);
        assertThat(result.output().get("result").decimalValue()).isEqualByComparingTo(new BigDecimal("2.5"));
    }

    @Test
    void rejectsDivisionByZero() throws Exception {
        var result = executor.execute(context, input("DIVIDE", 1, 0));
        assertThat(result.status()).isEqualTo(ToolExecutionOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo("TOOL_INPUT_INVALID");
    }

    @Test
    void rejectsInvalidOperationEnum() throws Exception {
        var result = executor.execute(context, input("POWER", 2, 3));
        assertThat(result.status()).isEqualTo(ToolExecutionOutcome.FAILED);
        assertThat(result.errorCode()).isEqualTo("TOOL_INPUT_INVALID");
    }

    private ObjectNode input(String operation, double left, double right) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("operation", operation);
        node.put("left", left);
        node.put("right", right);
        return node;
    }
}
