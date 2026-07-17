package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.tool.validation.ToolInputValidator;
import ai.nova.platform.tool.validation.ToolValidationException;

class ToolInputValidatorTest {

    private ToolInputValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new ToolInputValidator();
        objectMapper = new ObjectMapper();
    }

    @Test
    void validatesRequiredFields() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"timezone":{"type":"string","maxLength":100}},"required":["timezone"],"additionalProperties":false}
                """);
        ObjectNode input = objectMapper.createObjectNode();
        input.put("timezone", "UTC");

        assertThatCode(() -> validator.validate(schema, input)).doesNotThrowAnyException();
    }

    @Test
    void rejectsMissingRequiredField() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"timezone":{"type":"string"}},"required":["timezone"],"additionalProperties":false}
                """);
        ObjectNode input = objectMapper.createObjectNode();

        assertThatThrownBy(() -> validator.validate(schema, input))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("Missing required property");
    }

    @Test
    void rejectsAdditionalPropertiesWhenDisabled() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"a":{"type":"string"}},"required":["a"],"additionalProperties":false}
                """);
        ObjectNode input = objectMapper.createObjectNode();
        input.put("a", "x");
        input.put("b", "y");

        assertThatThrownBy(() -> validator.validate(schema, input))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("Unexpected property");
    }

    @Test
    void validatesStringMaxLength() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"text":{"type":"string","maxLength":3}},"required":["text"],"additionalProperties":false}
                """);
        ObjectNode input = objectMapper.createObjectNode();
        input.put("text", "abcd");

        assertThatThrownBy(() -> validator.validate(schema, input))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("too long");
    }

    @Test
    void validatesEnumValues() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"operation":{"type":"string","enum":["ADD","SUBTRACT"]}},"required":["operation"],"additionalProperties":false}
                """);
        ObjectNode input = objectMapper.createObjectNode();
        input.put("operation", "MULTIPLY");

        assertThatThrownBy(() -> validator.validate(schema, input))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("Invalid enum value");
    }

    @Test
    void validatesNumberMinimumMaximum() throws Exception {
        JsonNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"value":{"type":"number","minimum":0,"maximum":10}},"required":["value"],"additionalProperties":false}
                """);
        ObjectNode badLow = objectMapper.createObjectNode();
        badLow.put("value", -1);
        assertThatThrownBy(() -> validator.validate(schema, badLow))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("below minimum");

        ObjectNode badHigh = objectMapper.createObjectNode();
        badHigh.put("value", 11);
        assertThatThrownBy(() -> validator.validate(schema, badHigh))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("above maximum");
    }
}
