package ai.nova.platform.tool.executor;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.tool.validation.ToolInputValidator;
import ai.nova.platform.tool.validation.ToolValidationException;

@Component
public class CurrentDateTimeToolExecutor implements ToolExecutor {

    public static final Set<String> ALLOWED_TIMEZONES = Set.of(
            "Asia/Riyadh", "UTC", "America/New_York", "Europe/London", "Asia/Dubai");

    private static final String INPUT_SCHEMA = """
            {"type":"object","properties":{"timezone":{"type":"string","maxLength":100}},"required":["timezone"],"additionalProperties":false}
            """;

    private final ObjectMapper objectMapper;
    private final ToolInputValidator inputValidator;
    private final JsonNode schema;

    public CurrentDateTimeToolExecutor(ObjectMapper objectMapper, ToolInputValidator inputValidator) {
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
        return "CURRENT_DATETIME";
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode input) {
        long start = System.nanoTime();
        try {
            inputValidator.validate(schema, input);
            String timezone = input.get("timezone").asText();
            if (!ALLOWED_TIMEZONES.contains(timezone)) {
                return ToolExecutionResult.failure("TOOL_INPUT_INVALID", elapsedMs(start));
            }
            Instant now = Instant.now();
            String isoDateTime = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(
                    now.atZone(ZoneId.of(timezone)));
            ObjectNode output = objectMapper.createObjectNode();
            output.put("timezone", timezone);
            output.put("isoDateTime", isoDateTime);
            return ToolExecutionResult.success(output, elapsedMs(start));
        } catch (ToolValidationException ex) {
            return ToolExecutionResult.failure(ex.getCode(), elapsedMs(start));
        }
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
