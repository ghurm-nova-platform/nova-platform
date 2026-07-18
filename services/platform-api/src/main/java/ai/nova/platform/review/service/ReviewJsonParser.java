package ai.nova.platform.review.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFindingDraft;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;
import ai.nova.platform.web.error.ApiException;

@Service
public class ReviewJsonParser {

    private final ObjectMapper objectMapper;

    public ReviewJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedReviewOutput parse(String rawText) {
        String json = extractJson(rawText);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw invalid("Review output must be a JSON object");
            }
            String summary = text(root, "summary");
            if (summary == null || summary.isBlank()) {
                summary = "Review completed";
            }
            Integer score = root.path("score").isNumber() ? root.path("score").asInt() : null;
            Boolean approved = root.has("approved") && !root.get("approved").isNull()
                    ? root.get("approved").asBoolean()
                    : null;

            List<ReviewFindingDraft> findings = new ArrayList<>();
            JsonNode findingsNode = root.path("findings");
            if (findingsNode.isArray()) {
                for (JsonNode node : findingsNode) {
                    findings.add(parseFinding(node));
                }
            } else if (!findingsNode.isMissingNode() && !findingsNode.isNull()) {
                throw invalid("findings must be an array");
            }
            return new ParsedReviewOutput(summary.trim(), score, approved, List.copyOf(findings));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("Failed to parse review JSON: " + ex.getMessage());
        }
    }

    private ReviewFindingDraft parseFinding(JsonNode node) {
        ReviewSeverity severity = enumValue(node, "severity", ReviewSeverity.class);
        ReviewCategory category = enumValue(node, "category", ReviewCategory.class);
        String title = text(node, "title");
        String description = text(node, "description");
        String recommendation = text(node, "recommendation");
        String artifactPath = text(node, "artifactPath");
        if (artifactPath == null) {
            artifactPath = text(node, "path");
        }
        return new ReviewFindingDraft(severity, category, title, description, recommendation, artifactPath);
    }

    private static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw invalid("Review output is empty");
        }
        String trimmed = rawText.trim();
        if (trimmed.startsWith("```")) {
            int firstNl = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNl > 0 && lastFence > firstNl) {
                trimmed = trimmed.substring(firstNl + 1, lastFence).trim();
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start < 0 || end <= start) {
            throw invalid("Review output does not contain a JSON object");
        }
        return trimmed.substring(start, end + 1);
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        return value.asText();
    }

    private static <E extends Enum<E>> E enumValue(JsonNode node, String field, Class<E> type) {
        String raw = text(node, field);
        if (raw == null || raw.isBlank()) {
            throw invalid("Missing required field: " + field);
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        if ("ERRORHANDLING".equals(normalized) || "ERROR_HANDLING".equals(normalized)) {
            normalized = "ERROR_HANDLING";
        }
        if ("BESTPRACTICES".equals(normalized) || "BEST_PRACTICE".equals(normalized)) {
            normalized = "BEST_PRACTICES";
        }
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ex) {
            if (type == ReviewSeverity.class) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "REVIEW_UNKNOWN_SEVERITY", "Unknown severity: " + raw);
            }
            if (type == ReviewCategory.class) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "REVIEW_UNKNOWN_CATEGORY", "Unknown category: " + raw);
            }
            throw invalid("Invalid value for " + field + ": " + raw);
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "REVIEW_INVALID_JSON", message);
    }
}
