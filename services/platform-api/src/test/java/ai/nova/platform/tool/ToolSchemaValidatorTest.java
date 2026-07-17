package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.tool.validation.ToolSchemaValidator;
import ai.nova.platform.tool.validation.ToolValidationException;

class ToolSchemaValidatorTest {

    private ToolSchemaValidator validator;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        validator = new ToolSchemaValidator();
        objectMapper = new ObjectMapper();
    }

    @Test
    void acceptsValidObjectSchema() throws Exception {
        ObjectNode schema = objectMapper.createObjectNode();
        schema.put("type", "object");
        ObjectNode properties = schema.putObject("properties");
        ObjectNode timezone = properties.putObject("timezone");
        timezone.put("type", "string");
        timezone.put("maxLength", 100);
        schema.putArray("required").add("timezone");
        schema.put("additionalProperties", false);

        validator.validateStructure(schema);
    }

    @Test
    void rejectsRefKeyword() throws Exception {
        ObjectNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"x":{"$ref":"#/definitions/X"}},"required":["x"]}
                """).deepCopy();
        assertThatThrownBy(() -> validator.validateStructure(schema))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("$ref");
    }

    @Test
    void rejectsOneOfKeyword() throws Exception {
        ObjectNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"x":{"oneOf":[{"type":"string"}]}},"required":["x"]}
                """).deepCopy();
        assertThatThrownBy(() -> validator.validateStructure(schema))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("oneOf");
    }

    @Test
    void rejectsPatternKeyword() throws Exception {
        ObjectNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"x":{"type":"string","pattern":"^[a-z]+$"}},"required":["x"]}
                """).deepCopy();
        assertThatThrownBy(() -> validator.validateStructure(schema))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("pattern");
    }

    @Test
    void rejectsSchemaExceedingMaxLength() {
        String longSchema = "{\"type\":\"object\",\"properties\":{\"a\":{\"type\":\"string\"}},\"required\":[\"a\"]}";
        assertThatThrownBy(() -> validator.validate(longSchema, 10))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("maximum length");
    }

    @Test
    void rejectsRequiredPropertyNotDefined() throws Exception {
        ObjectNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"a":{"type":"string"}},"required":["missing"]}
                """).deepCopy();
        assertThatThrownBy(() -> validator.validateStructure(schema))
                .isInstanceOf(ToolValidationException.class)
                .hasMessageContaining("required property not defined");
    }

    @Test
    void acceptsEnumOnStringProperty() throws Exception {
        ObjectNode schema = objectMapper.readTree("""
                {"type":"object","properties":{"op":{"type":"string","enum":["ADD","SUBTRACT"]}},"required":["op"]}
                """).deepCopy();
        validator.validateStructure(schema);
    }
}
