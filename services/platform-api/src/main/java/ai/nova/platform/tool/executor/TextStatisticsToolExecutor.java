package ai.nova.platform.tool.executor;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.tool.validation.ToolInputValidator;
import ai.nova.platform.tool.validation.ToolValidationException;

@Component
public class TextStatisticsToolExecutor implements ToolExecutor {

    private static final String INPUT_SCHEMA = """
            {"type":"object","properties":{"text":{"type":"string","maxLength":10000}},"required":["text"],"additionalProperties":false}
            """;

    private final ObjectMapper objectMapper;
    private final ToolInputValidator inputValidator;
    private final JsonNode schema;

    public TextStatisticsToolExecutor(ObjectMapper objectMapper, ToolInputValidator inputValidator) {
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
        return "TEXT_STATISTICS";
    }

    @Override
    public ToolExecutionResult execute(ToolExecutionContext context, JsonNode input) {
        long start = System.nanoTime();
        try {
            inputValidator.validate(schema, input);
            String text = input.get("text").asText();
            int characters = text.length();
            int charactersWithoutSpaces = text.replaceAll("\\s", "").length();
            int words = countWords(text);
            int lines = countLines(text);

            ObjectNode output = objectMapper.createObjectNode();
            output.put("characters", characters);
            output.put("charactersWithoutSpaces", charactersWithoutSpaces);
            output.put("words", words);
            output.put("lines", lines);
            return ToolExecutionResult.success(output, elapsedMs(start));
        } catch (ToolValidationException ex) {
            return ToolExecutionResult.failure(ex.getCode(), elapsedMs(start));
        }
    }

    private int countWords(String text) {
        if (text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }

    private int countLines(String text) {
        if (text.isEmpty()) {
            return 0;
        }
        return text.split("\\R", -1).length;
    }

    private long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }
}
