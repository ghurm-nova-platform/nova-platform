package ai.nova.platform.tool.validation;

import java.util.Iterator;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

@Component
public class ToolSchemaValidator {

    private static final Set<String> ROOT_TYPES = Set.of("object");
    private static final Set<String> PROPERTY_TYPES =
            Set.of("object", "string", "number", "integer", "boolean");
    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "$ref", "oneOf", "anyOf", "allOf", "pattern", "patternProperties", "definitions", "$defs");

    public void validate(String schemaJson, int maxCharacters) {
        if (schemaJson == null || schemaJson.isBlank()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Schema must not be blank");
        }
        if (schemaJson.length() > maxCharacters) {
            throw new ToolValidationException(
                    "TOOL_SCHEMA_TOO_LONG",
                    "Schema exceeds maximum length of " + maxCharacters);
        }
    }

    public void validateStructure(JsonNode schema) {
        if (schema == null || !schema.isObject()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Schema root must be an object");
        }
        rejectForbiddenKeys(schema);
        String type = requireType(schema, ROOT_TYPES, "Schema root");
        if (!"object".equals(type)) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Schema root type must be object");
        }
        validateObjectSchema(schema, true);
    }

    private void validateObjectSchema(JsonNode schema, boolean root) {
        JsonNode properties = schema.get("properties");
        if (properties == null || !properties.isObject()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Object schema must define properties");
        }
        if (properties.isEmpty() && root) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Schema properties must not be empty");
        }

        Iterator<String> fieldNames = properties.fieldNames();
        while (fieldNames.hasNext()) {
            validatePropertySchema(properties.get(fieldNames.next()));
        }

        JsonNode required = schema.get("required");
        if (required != null) {
            if (!required.isArray()) {
                throw new ToolValidationException("TOOL_SCHEMA_INVALID", "required must be an array");
            }
            for (JsonNode item : required) {
                if (!item.isTextual()) {
                    throw new ToolValidationException("TOOL_SCHEMA_INVALID", "required entries must be strings");
                }
                if (!properties.has(item.asText())) {
                    throw new ToolValidationException(
                            "TOOL_SCHEMA_INVALID", "required property not defined: " + item.asText());
                }
            }
        }

        JsonNode additionalProperties = schema.get("additionalProperties");
        if (additionalProperties != null && !additionalProperties.isBoolean()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "additionalProperties must be a boolean");
        }
    }

    private void validatePropertySchema(JsonNode property) {
        if (!property.isObject()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Property schema must be an object");
        }
        rejectForbiddenKeys(property);
        String type = requireType(property, PROPERTY_TYPES, "Property");
        switch (type) {
            case "object" -> validateObjectSchema(property, false);
            case "string" -> validateStringSchema(property);
            case "number", "integer" -> validateNumberSchema(property, type);
            case "boolean" -> {
                // no extra constraints supported
            }
            default -> throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Unsupported type: " + type);
        }
    }

    private void validateStringSchema(JsonNode property) {
        JsonNode maxLength = property.get("maxLength");
        if (maxLength != null && (!maxLength.isInt() || maxLength.asInt() < 0)) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "maxLength must be a non-negative integer");
        }
        JsonNode enumNode = property.get("enum");
        if (enumNode != null) {
            validateEnum(enumNode, true);
        }
    }

    private void validateNumberSchema(JsonNode property, String type) {
        JsonNode minimum = property.get("minimum");
        if (minimum != null && !minimum.isNumber()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "minimum must be a number");
        }
        JsonNode maximum = property.get("maximum");
        if (maximum != null && !maximum.isNumber()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "maximum must be a number");
        }
        if (minimum != null && maximum != null && minimum.decimalValue().compareTo(maximum.decimalValue()) > 0) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "minimum must not exceed maximum");
        }
        if ("integer".equals(type)) {
            JsonNode enumNode = property.get("enum");
            if (enumNode != null) {
                validateEnum(enumNode, false);
            }
        }
    }

    private void validateEnum(JsonNode enumNode, boolean stringsOnly) {
        if (!enumNode.isArray() || enumNode.isEmpty()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", "enum must be a non-empty array");
        }
        for (JsonNode value : enumNode) {
            if (stringsOnly && !value.isTextual()) {
                throw new ToolValidationException("TOOL_SCHEMA_INVALID", "enum values must be strings");
            }
            if (!stringsOnly && !value.isInt()) {
                throw new ToolValidationException("TOOL_SCHEMA_INVALID", "enum values must be integers");
            }
        }
    }

    private String requireType(JsonNode node, Set<String> allowed, String context) {
        JsonNode typeNode = node.get("type");
        if (typeNode == null || !typeNode.isTextual()) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", context + " must declare type");
        }
        String type = typeNode.asText();
        if (!allowed.contains(type)) {
            throw new ToolValidationException("TOOL_SCHEMA_INVALID", context + " has unsupported type: " + type);
        }
        return type;
    }

    private void rejectForbiddenKeys(JsonNode node) {
        Iterator<String> names = node.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (FORBIDDEN_KEYS.contains(name)) {
                throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Unsupported schema keyword: " + name);
            }
            if (name.startsWith("$")) {
                throw new ToolValidationException("TOOL_SCHEMA_INVALID", "Unsupported schema keyword: " + name);
            }
            JsonNode child = node.get(name);
            if (child != null && child.isObject()) {
                rejectForbiddenKeys(child);
            } else if (child != null && child.isArray()) {
                for (JsonNode item : child) {
                    if (item.isObject()) {
                        rejectForbiddenKeys(item);
                    }
                }
            }
        }
    }
}
