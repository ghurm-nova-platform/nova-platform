package ai.nova.platform.review.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.review.config.ReviewProperties;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFindingDraft;
import ai.nova.platform.web.error.ApiException;

@Service
public class ReviewValidator {

    private final ReviewProperties properties;

    public ReviewValidator(ReviewProperties properties) {
        this.properties = properties;
    }

    public void validate(ParsedReviewOutput output) {
        if (output == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_INVALID_JSON", "Review output is required");
        }
        if (output.score() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_INVALID_SCORE", "Review score is required");
        }
        if (output.score() < 0 || output.score() > 100) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "REVIEW_INVALID_SCORE", "Review score must be between 0 and 100");
        }
        if (output.approved() == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "REVIEW_INVALID_JSON", "Review approved flag is required");
        }
        if (output.findings() == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_INVALID_JSON", "findings must be present");
        }
        if (output.findings().size() > properties.getMaxFindings()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "REVIEW_TOO_MANY_FINDINGS",
                    "Too many findings (max " + properties.getMaxFindings() + ")");
        }
        for (ReviewFindingDraft finding : output.findings()) {
            if (finding.severity() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "REVIEW_UNKNOWN_SEVERITY", "Finding severity is required");
            }
            if (finding.category() == null) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "REVIEW_UNKNOWN_CATEGORY", "Finding category is required");
            }
            if (finding.title() == null || finding.title().isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "REVIEW_MISSING_TITLE", "Finding title is required");
            }
            if (finding.recommendation() == null || finding.recommendation().isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "REVIEW_MISSING_RECOMMENDATION",
                        "Finding recommendation is required");
            }
            if (finding.description() == null || finding.description().isBlank()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "REVIEW_INVALID_JSON", "Finding description is required");
            }
        }
    }
}
