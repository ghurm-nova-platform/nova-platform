package ai.nova.platform.tool.validation;

import java.math.BigDecimal;
import java.util.Iterator;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class ToolInputValidator {

    public void validate(JsonNode schema, JsonNode input) {
        if (schema == null || !schema.isObject()) {
            throw new ToolValidationException("TOOL_INPUT_INVALID", "Schema must be an object");
        }
        if (input == null || !input.isObject()) {
            throw new ToolValidationException("TOOL_INPUT_INVALID", "Input must be a JSON object");
        }
        validateObject(schema, input, "$");
    }

    private void validateObject(JsonNode schema, JsonNode input, String path) {
        JsonNode properties = schema.get("properties");
        if (properties == null || !properties.isObject()) {
            throw new ToolValidationException("TOOL_INPUT_INVALID", "Schema object missing properties at " + path);
        }

        JsonNode additionalProperties = schema.get("additionalProperties");
        boolean allowAdditional = additionalProperties == null || additionalProperties.asBoolean(true);
        if (!allowAdditional) {
            Iterator<String> inputFields = input.fieldNames();
            while (inputFields.hasNext()) {
                String field = inputFields.next();
                if (!properties.has(field)) {
                    throw new ToolValidationException(
                            "TOOL_INPUT_INVALID", "Unexpected property at " + path + "." + field);
                }
            }
        }

        JsonNode required = schema.get("required");
        if (required != null && required.isArray()) {
            for (JsonNode requiredField : required) {
                String name = requiredField.asText();
                JsonNode value = input.get(name);
                if (value == null || value.isNull()) {
                    throw new ToolValidationException(
                            "TOOL_INPUT_INVALID", "Missing required property at " + path + "." + name);
                }
            }
        }

        Iterator<String> propertyNames = properties.fieldNames();
        while (propertyNames.hasNext()) {
            String name = propertyNames.next();
            JsonNode value = input.get(name);
            if (value != null && !value.isNull()) {
                validateValue(properties.get(name), value, path + "." + name);
            }
        }
    }

    private void validateValue(JsonNode schema, JsonNode value, String path) {
        String type = schema.get("type").asText();
        switch (type) {
            case "object" -> {
                if (!value.isObject()) {
                    throw typeMismatch(path, "object");
                }
                validateObject(schema, value, path);
            }
            case "string" -> validateString(schema, value, path);
            case "number" -> validateNumber(schema, value, path, false);
            case "integer" -> validateNumber(schema, value, path, true);
            case "boolean" -> {
                if (!value.isBoolean()) {
                    throw typeMismatch(path, "boolean");
                }
            }
            default -> throw new ToolValidationException("TOOL_INPUT_INVALID", "Unsupported type at " + path);
        }
    }

    private void validateString(JsonNode schema, JsonNode value, String path) {
        if (!value.isTextual()) {
            throw typeMismatch(path, "string");
        }
        String text = value.asText();
        JsonNode maxLength = schema.get("maxLength");
        if (maxLength != null && text.length() > maxLength.asInt()) {
            throw new ToolValidationException(
                    "TOOL_INPUT_INVALID", "String too long at " + path + " (max " + maxLength.asInt() + ")");
        }
        JsonNode enumNode = schema.get("enum");
        if (enumNode != null) {
            boolean matched = false;
            for (JsonNode allowed : enumNode) {
                if (allowed.asText().equals(text)) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new ToolValidationException("TOOL_INPUT_INVALID", "Invalid enum value at " + path);
            }
        }
    }

    private void validateNumber(JsonNode schema, JsonNode value, String path, boolean integer) {
        if (integer && !value.isIntegralNumber()) {
            throw typeMismatch(path, "integer");
        }
        if (!integer && !value.isNumber()) {
            throw typeMismatch(path, "number");
        }
        BigDecimal number = value.decimalValue();
        JsonNode minimum = schema.get("minimum");
        if (minimum != null && number.compareTo(minimum.decimalValue()) < 0) {
            throw new ToolValidationException("TOOL_INPUT_INVALID", "Value below minimum at " + path);
        }
        JsonNode maximum = schema.get("maximum");
        if (maximum != null && number.compareTo(maximum.decimalValue()) > 0) {
            throw new ToolValidationException("TOOL_INPUT_INVALID", "Value above maximum at " + path);
        }
        JsonNode enumNode = schema.get("enum");
        if (enumNode != null) {
            boolean matched = false;
            for (JsonNode allowed : enumNode) {
                if (allowed.decimalValue().compareTo(number) == 0) {
                    matched = true;
                    break;
                }
            }
            if (!matched) {
                throw new ToolValidationException("TOOL_INPUT_INVALID", "Invalid enum value at " + path);
            }
        }
    }

    private ToolValidationException typeMismatch(String path, String expected) {
        return new ToolValidationException("TOOL_INPUT_INVALID", "Expected " + expected + " at " + path);
    }
}
