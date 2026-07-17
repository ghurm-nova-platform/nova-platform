package ai.nova.platform.prompt.parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class PromptVariableParser {

    private static final Pattern VALID_PLACEHOLDER =
            Pattern.compile("\\{\\{\\s*([a-zA-Z_][a-zA-Z0-9_]*)\\s*\\}\\}");
    private static final Pattern ANY_OPEN = Pattern.compile("\\{\\{");
    private static final Pattern ANY_CLOSE = Pattern.compile("\\}\\}");

    public List<String> detectVariables(String content) {
        if (!StringUtils.hasText(content)) {
            validateBraceBalance(content);
            return List.of();
        }
        validateSyntax(content);
        LinkedHashSet<String> names = new LinkedHashSet<>();
        Matcher matcher = VALID_PLACEHOLDER.matcher(content);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return List.copyOf(names);
    }

    public ValidationResult validate(String content, List<VariableDefinition> variableDefs) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (!StringUtils.hasText(content) || content.trim().isEmpty()) {
            errors.add("Prompt content must not be blank");
            return new ValidationResult(false, List.of(), errors, warnings);
        }

        try {
            validateSyntax(content);
        } catch (SyntaxException ex) {
            errors.add(ex.getMessage());
            return new ValidationResult(false, List.of(), errors, warnings);
        }

        List<String> detected = detectVariables(content);
        Map<String, VariableDefinition> defsByName = indexDefinitions(variableDefs);

        for (String name : detected) {
            if (!defsByName.containsKey(name)) {
                warnings.add("Variable '" + name + "' is used in content but not defined");
            }
        }

        for (VariableDefinition def : variableDefs) {
            if (def.required() && !detected.contains(def.name())) {
                errors.add("Required variable '" + def.name() + "' is not referenced in content");
            }
        }

        for (VariableDefinition def : variableDefs) {
            if (!def.required() && !detected.contains(def.name())) {
                warnings.add("Defined variable '" + def.name() + "' is not used in content");
            }
        }

        Set<String> duplicateCheck = new HashSet<>();
        for (VariableDefinition def : variableDefs) {
            if (!duplicateCheck.add(def.name())) {
                errors.add("Duplicate variable definition: " + def.name());
            }
        }

        return new ValidationResult(errors.isEmpty(), detected, List.copyOf(errors), List.copyOf(warnings));
    }

    public PreviewResult preview(String content, Map<String, String> values, List<VariableDefinition> variableDefs) {
        ValidationResult validation = validate(content, variableDefs);
        if (!validation.valid()) {
            return new PreviewResult(null, validation.errors(), validation.warnings(), List.of());
        }

        Map<String, VariableDefinition> defsByName = indexDefinitions(variableDefs);
        List<String> missingRequired = new ArrayList<>();
        List<String> warnings = new ArrayList<>(validation.warnings());

        String rendered = content;
        for (String name : detectVariables(content)) {
            VariableDefinition def = defsByName.get(name);
            String value = values != null ? values.get(name) : null;
            if (!StringUtils.hasText(value) && def != null && StringUtils.hasText(def.defaultValue())) {
                value = def.defaultValue();
            }
            if (!StringUtils.hasText(value)) {
                if (def != null && def.required()) {
                    missingRequired.add(name);
                }
                continue;
            }
            rendered = replacePlaceholder(rendered, name, value);
        }

        if (!missingRequired.isEmpty()) {
            return new PreviewResult(null, List.of(), warnings, List.copyOf(missingRequired));
        }

        return new PreviewResult(rendered, List.of(), warnings, List.of());
    }

    private static Map<String, VariableDefinition> indexDefinitions(List<VariableDefinition> variableDefs) {
        Map<String, VariableDefinition> defsByName = new LinkedHashMap<>();
        if (variableDefs != null) {
            for (VariableDefinition def : variableDefs) {
                defsByName.put(def.name(), def);
            }
        }
        return defsByName;
    }

    private static String replacePlaceholder(String content, String name, String value) {
        Pattern pattern = Pattern.compile("\\{\\{\\s*" + Pattern.quote(name) + "\\s*\\}\\}");
        return pattern.matcher(content).replaceAll(Matcher.quoteReplacement(value));
    }

    private void validateSyntax(String content) {
        validateBraceBalance(content);
        scanForMalformedPlaceholders(content);
    }

    private static void validateBraceBalance(String content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        int depth = 0;
        for (int i = 0; i < content.length() - 1; i++) {
            if (content.charAt(i) == '{' && content.charAt(i + 1) == '{') {
                depth++;
                i++;
            } else if (content.charAt(i) == '}' && content.charAt(i + 1) == '}') {
                depth--;
                if (depth < 0) {
                    throw new SyntaxException("Unmatched closing braces '}}' in prompt content");
                }
                i++;
            }
        }
        if (depth > 0) {
            throw new SyntaxException("Unmatched opening braces '{{' in prompt content");
        }
    }

    private static void scanForMalformedPlaceholders(String content) {
        int index = 0;
        while (index < content.length()) {
            int open = content.indexOf("{{", index);
            if (open < 0) {
                break;
            }
            int close = content.indexOf("}}", open + 2);
            if (close < 0) {
                throw new SyntaxException("Unmatched opening braces '{{' in prompt content");
            }
            String inner = content.substring(open + 2, close);
            if (!inner.trim().matches("[a-zA-Z_][a-zA-Z0-9_]*")) {
                throw new SyntaxException("Malformed variable placeholder: {{" + inner + "}}");
            }
            index = close + 2;
        }
    }

    public record VariableDefinition(
            String name,
            boolean required,
            String defaultValue) {
    }

    public record ValidationResult(
            boolean valid,
            List<String> detectedVariables,
            List<String> errors,
            List<String> warnings) {
    }

    public record PreviewResult(
            String renderedContent,
            List<String> errors,
            List<String> warnings,
            List<String> missingRequiredVariables) {
    }

    static class SyntaxException extends RuntimeException {
        SyntaxException(String message) {
            super(message);
        }
    }
}
