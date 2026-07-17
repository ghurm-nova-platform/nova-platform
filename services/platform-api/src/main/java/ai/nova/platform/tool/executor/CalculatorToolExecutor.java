package ai.nova.platform.tool.executor;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.tool.validation.ToolInputValidator;
import ai.nova.platform.tool.validation.ToolValidationException;

@Component
public class CalculatorToolExecutor implements ToolExecutor {

    private static final String INPUT_SCHEMA = """
            {"type":"object","properties":{"operation":{"type":"string","enum":["ADD","SUBTRACT","MULTIPLY","DIVIDE"]},"left":{"type":"number"},"right":{"type":"number"}},"required":["operation","left","right"],"additionalProperties":false}
            """;

    private final ObjectMapper objectMapper;
    private final ToolInputValidator inputValidator;
    private final JsonNode schema;

    public CalculatorToolExecutor(ObjectMapper objectMapper, ToolInputValidator inputValidator) {
        this.objectMapper = objectMapper;
        this.inputValidator = inputValidator;
        try {
            this.schema = objectMapper.readTree(INPUT_SCHEMA);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid built-in schema", ex);
        }
    }

    @Override
    public String executorKey() {
        return "CALCULATOR";
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode input) {
        long start = System.nanoTime();
        try {
            inputValidator.validate(schema, input);
            String operation = input.get("operation").asText();
            BigDecimal left = input.get("left").decimalValue();
            BigDecimal right = input.get("right").decimalValue();
            BigDecimal result = compute(operation, left, right);
            ObjectNode output = objectMapper.createObjectNode();
            output.put("operation", operation);
            output.put("left", left);
            output.put("right", right);
            output.put("result", result);
            return ToolExecutionResult.success(output, elapsedMs(start));
        } catch (ToolValidationException ex) {
            return ToolExecutionResult.failure(ex.getCode(), elapsedMs(start));
        } catch (ArithmeticException ex) {
            return ToolExecutionResult.failure("TOOL_EXECUTION_FAILED", elapsedMs(start));
        }
    }

    private BigDecimal compute(String operation, BigDecimal left, BigDecimal right) {
        return switch (operation) {
            case "ADD" -> left.add(right);
            case "SUBTRACT" -> left.subtract(right);
            case "MULTIPLY" -> left.multiply(right);
            case "DIVIDE" -> {
                if (right.compareTo(BigDecimal.ZERO) == 0) {
                    throw new ToolValidationException("TOOL_INPUT_INVALID", "Division by zero");
                }
                yield left.divide(right, 10, RoundingMode.HALF_UP);
            }
            default -> throw new ToolValidationException("TOOL_INPUT_INVALID", "Unsupported operation");
        };
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
