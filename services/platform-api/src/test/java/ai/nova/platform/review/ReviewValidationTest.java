package ai.nova.platform.review;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.review.config.ReviewProperties;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.service.ReviewJsonParser;
import ai.nova.platform.review.service.ReviewValidator;
import ai.nova.platform.web.error.ApiException;

class ReviewValidationTest {

    private ReviewJsonParser parser;
    private ReviewValidator validator;

    @BeforeEach
    void setUp() {
        parser = new ReviewJsonParser(new ObjectMapper());
        validator = new ReviewValidator(new ReviewProperties());
    }

    @Test
    void rejectsUnknownSeverity() {
        assertThatThrownBy(() -> parser.parse("""
                {"summary":"x","score":80,"approved":true,"findings":[
                  {"severity":"EXTREME","category":"SECURITY","title":"t","description":"d","recommendation":"r"}
                ]}
                """))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_UNKNOWN_SEVERITY");
    }

    @Test
    void rejectsUnknownCategory() {
        assertThatThrownBy(() -> parser.parse("""
                {"summary":"x","score":80,"approved":true,"findings":[
                  {"severity":"LOW","category":"SMELL","title":"t","description":"d","recommendation":"r"}
                ]}
                """))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_UNKNOWN_CATEGORY");
    }

    @Test
    void rejectsMissingTitle() {
        ParsedReviewOutput output = parser.parse("""
                {"summary":"x","score":80,"approved":true,"findings":[
                  {"severity":"LOW","category":"SECURITY","title":" ","description":"d","recommendation":"r"}
                ]}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_MISSING_TITLE");
    }

    @Test
    void rejectsMissingRecommendation() {
        ParsedReviewOutput output = parser.parse("""
                {"summary":"x","score":80,"approved":true,"findings":[
                  {"severity":"LOW","category":"SECURITY","title":"t","description":"d","recommendation":""}
                ]}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_MISSING_RECOMMENDATION");
    }

    @Test
    void rejectsInvalidScore() {
        ParsedReviewOutput output = parser.parse("""
                {"summary":"x","score":140,"approved":true,"findings":[]}
                """);
        assertThatThrownBy(() -> validator.validate(output))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_INVALID_SCORE");
    }

    @Test
    void rejectsInvalidJson() {
        assertThatThrownBy(() -> parser.parse("not-json"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REVIEW_INVALID_JSON");
    }

    @Test
    void acceptsValidReview() {
        ParsedReviewOutput output = parser.parse("""
                {
                  "summary":"Overall code quality is good.",
                  "score":92,
                  "approved":true,
                  "findings":[
                    {
                      "severity":"MEDIUM",
                      "category":"Security",
                      "title":"Input validation",
                      "description":"Request payload is not validated.",
                      "recommendation":"Add Bean Validation."
                    }
                  ]
                }
                """);
        validator.validate(output);
        assertThat(output.score()).isEqualTo(92);
        assertThat(output.approved()).isTrue();
        assertThat(output.findings()).hasSize(1);
        assertThat(output.findings().get(0).category().name()).isEqualTo("SECURITY");
    }
}
