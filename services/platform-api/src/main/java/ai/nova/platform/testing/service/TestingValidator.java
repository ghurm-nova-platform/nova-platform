package ai.nova.platform.testing.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.testing.config.TestingProperties;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTestDraft;
import ai.nova.platform.testing.dto.TestingDtos.ParsedTestingOutput;
import ai.nova.platform.testing.dto.TestingDtos.TestCaseDraft;
import ai.nova.platform.web.error.ApiException;

@Service
public class TestingValidator {

    private final TestingProperties properties;

    public TestingValidator(TestingProperties properties) {
        this.properties = properties;
    }

    public void validate(ParsedTestingOutput output) {
        if (output == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "TESTING_INVALID_JSON", "Testing output is required");
        }
        if (output.coverageEstimate() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "TESTING_INVALID_COVERAGE", "Coverage estimate is required");
        }
        if (output.coverageEstimate() < 0 || output.coverageEstimate() > 100) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TESTING_INVALID_COVERAGE",
                    "Coverage estimate must be between 0 and 100");
        }
        if (output.generatedTests() == null || output.generatedTests().isEmpty()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "TESTING_EMPTY_TESTS", "generatedTests must not be empty");
        }
        if (output.generatedTests().size() > properties.getMaxTests()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "TESTING_TOO_MANY_TESTS",
                    "Too many generated tests (max " + properties.getMaxTests() + ")");
        }
        for (GeneratedTestDraft test : output.generatedTests()) {
            if (test.type() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "TESTING_UNKNOWN_TYPE", "Test type is required");
            }
            if (test.priority() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "TESTING_UNKNOWN_PRIORITY", "Test priority is required");
            }
            if (test.title() == null || test.title().isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "TESTING_MISSING_TITLE", "Test title is required");
            }
            if (test.description() == null || test.description().isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "TESTING_MISSING_DESCRIPTION", "Test description is required");
            }
            if (test.cases() != null && test.cases().size() > properties.getMaxCasesPerTest()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "TESTING_TOO_MANY_CASES",
                        "Too many cases for test (max " + properties.getMaxCasesPerTest() + ")");
            }
            if (test.cases() != null) {
                for (TestCaseDraft testCase : test.cases()) {
                    if (testCase.name() == null || testCase.name().isBlank()) {
                        throw new ApiException(
                                HttpStatus.BAD_REQUEST, "TESTING_MISSING_TITLE", "Test case name is required");
                    }
                    if (testCase.priority() == null) {
                        throw new ApiException(
                                HttpStatus.BAD_REQUEST,
                                "TESTING_UNKNOWN_PRIORITY",
                                "Test case priority is required");
                    }
                }
            }
        }
    }
}
