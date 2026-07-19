package ai.nova.platform.approval.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.web.error.ApiException;

@Service
public class ApprovalCommentSanitizer {

    private final ApprovalGateProperties properties;

    public ApprovalCommentSanitizer(ApprovalGateProperties properties) {
        this.properties = properties;
    }

    public String sanitizeRequired(String comment, boolean required) {
        if (comment == null || comment.isBlank()) {
            if (required) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "APPROVAL_COMMENT_REQUIRED", "Comment is required for rejection");
            }
            return null;
        }
        return sanitize(comment);
    }

    public String sanitize(String comment) {
        if (comment == null) {
            return null;
        }
        String trimmed = comment.trim();
        if (trimmed.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "APPROVAL_COMMENT_REQUIRED", "Comment cannot be blank");
        }
        if (containsUnsafeMarkup(trimmed)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "APPROVAL_COMMENT_UNSAFE", "Comment contains disallowed HTML or script content");
        }
        if (trimmed.length() > properties.getMaximumCommentLength()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "APPROVAL_COMMENT_TOO_LONG",
                    "Comment exceeds maximum length of " + properties.getMaximumCommentLength());
        }
        return trimmed;
    }

    private static boolean containsUnsafeMarkup(String value) {
        String lower = value.toLowerCase();
        return lower.contains("<script") || lower.contains("</script") || lower.contains("<iframe") || lower.contains("javascript:");
    }
}
