package ai.nova.platform.prompt.parser;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nova.platform.prompt.parser.PromptVariableParser.VariableDefinition;

class PromptVariableParserTest {

    private PromptVariableParser parser;

    @BeforeEach
    void setUp() {
        parser = new PromptVariableParser();
    }

    @Test
    void detectsValidVariables() {
        assertThat(parser.detectVariables("Hello {{customer_name}}, topic: {{topic}}"))
                .containsExactly("customer_name", "topic");
    }

    @Test
    void rejectsMalformedPlaceholders() {
        assertThatThrownBy(() -> parser.detectVariables("Bad {{bad-name}}"))
                .isInstanceOf(PromptVariableParser.SyntaxException.class)
                .hasMessageContaining("Malformed");

        assertThatThrownBy(() -> parser.detectVariables("Bad {{1x}}"))
                .isInstanceOf(PromptVariableParser.SyntaxException.class);

        assertThatThrownBy(() -> parser.detectVariables("Unclosed {{"))
                .isInstanceOf(PromptVariableParser.SyntaxException.class)
                .hasMessageContaining("Unmatched");
    }

    @Test
    void validatesContentAndDefinitions() {
        var result = parser.validate(
                "Hello {{name}}",
                List.of(new VariableDefinition("name", true, null)));

        assertThat(result.valid()).isTrue();
        assertThat(result.detectedVariables()).containsExactly("name");
    }

    @Test
    void reportsMissingRequiredDefinitions() {
        var result = parser.validate(
                "Hello {{name}}",
                List.of(new VariableDefinition("other", true, null)));

        assertThat(result.valid()).isFalse();
        assertThat(result.errors()).anyMatch(error -> error.contains("other"));
    }

    @Test
    void previewsWithSubstitution() {
        var result = parser.preview(
                "Hello {{name}}, issue: {{topic}}",
                Map.of("name", "Alex", "topic", "billing"),
                List.of(
                        new VariableDefinition("name", true, null),
                        new VariableDefinition("topic", true, null)));

        assertThat(result.renderedContent()).isEqualTo("Hello Alex, issue: billing");
        assertThat(result.missingRequiredVariables()).isEmpty();
    }

    @Test
    void previewUsesDefaultValuesAndReportsMissingRequired() {
        var withDefault = parser.preview(
                "Hello {{name}}",
                Map.of(),
                List.of(new VariableDefinition("name", false, "Guest")));

        assertThat(withDefault.renderedContent()).isEqualTo("Hello Guest");

        var missing = parser.preview(
                "Hello {{name}}",
                Map.of(),
                List.of(new VariableDefinition("name", true, null)));

        assertThat(missing.renderedContent()).isNull();
        assertThat(missing.missingRequiredVariables()).containsExactly("name");
    }

    @Test
    void warnsAboutUnusedDefinedVariables() {
        var result = parser.validate(
                "Static text",
                List.of(new VariableDefinition("unused", false, null)));

        assertThat(result.valid()).isTrue();
        assertThat(result.warnings()).anyMatch(w -> w.contains("unused"));
    }
}
