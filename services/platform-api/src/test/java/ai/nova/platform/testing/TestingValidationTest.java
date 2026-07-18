package ai.nova.platform.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.testing.config.TestingProperties;
import ai.nova.platform.testing.dto.TestingDtos.ParsedTestingOutput;
import ai.nova.platform.testing.service.TestingJsonParser;
import ai.nova.platform.testing.service.TestingValidator;
import ai.nova.platform.web.error.ApiException;

class TestingValidationTest {

    private TestingJsonParser parser;
    private TestingValidator validator;

    @BeforeEach
    void setUp() {
        parser = new TestingJsonParser(new ObjectMapper());
        validator = new TestingValidator(new TestingProperties());
    }

    @Test
    void rejectsUnknownType() {
        assertThatThrownBy(() -> parser.parse("""
                {"summary":"x","coverageEstimate":80,"generatedTests":[
                  {"type":"FUZZ","priority":"HIGH","title":"t","description":"d"}
                ]}
                """))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("TESTING_UNKNOWN_TYPE");
    }

    @Test
    void rejectsUnknownPriority() {
        assertThatThrownBy(() -> parser.parse("""
                {"summary":"x","coverageEstimate":80,"generatedTests":[
                  {"type":"UNIT","priority":"URGENT","title":"t","description":"d"}
                ]}
                """))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("TESTING_UNKNOWN_PRIORITY");
    }

    @Test
    void rejectsInvalidCoverage() {
        ParsedTestingOutput output = parser.parse("""
                {"summary":"x","coverageEstimate":140,"generatedTests":[
                  {"type":"UNIT","priority":"HIGH","title":"t","description":"d"}
                ]}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("TESTING_INVALID_COVERAGE");
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not-json"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("TESTING_INVALID_JSON");
    }

    @Test
    void rejectsMissingTitle() {
        ParsedTestingOutput output = parser.parse("""
                {"summary":"x","coverageEstimate":80,"generatedTests":[
                  {"type":"UNIT","priority":"HIGH","title":" ","description":"d"}
                ]}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("TESTING_MISSING_TITLE");
    }

    @Test
    void rejectsMissingDescription() {
        ParsedTestingOutput output = parser.parse("""
                {"summary":"x","coverageEstimate":80,"generatedTests":[
                  {"type":"UNIT","priority":"HIGH","title":"t","description":""}
                ]}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("TESTING_MISSING_DESCRIPTION");
    }

    @Test
    void acceptsValidPlan() {
        ParsedTestingOutput output = parser.parse("""
                {
                  "summary":"Unit and API tests generated.",
                  "coverageEstimate":84,
                  "generatedTests":[
                    {
                      "type":"UNIT",
                      "priority":"HIGH",
                      "title":"LoginService validation",
                      "description":"Verify invalid credentials."
                    }
                  ]
                }
                """);
        validator.validate(output);
        assertThat(output.coverageEstimate()).isEqualTo(84);
        assertThat(output.generatedTests()).hasSize(1);
    }
}
