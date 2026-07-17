package ai.nova.platform.knowledge.extractor;

import org.springframework.http.HttpStatus;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.web.error.ApiException;

public final class TextNormalization {

    private TextNormalization() {
    }

    public static String normalizeAndValidate(String text, KnowledgeProperties properties) {
        if (text == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_EMPTY", "Document content is empty");
        }
        String normalized = text.replace("\r\n", "\n").replace('\r', '\n').replace("\u0000", "");
        if (normalized.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "DOCUMENT_EMPTY", "Document content is empty");
        }
        if (looksBinary(normalized)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "DOCUMENT_BINARY", "Document appears to contain binary content");
        }
        if (normalized.length() > properties.getMaxExtractedCharacters()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DOCUMENT_TOO_LARGE",
                    "Extracted text exceeds maximum allowed characters");
        }
        return normalized;
    }

    static boolean looksBinary(String text) {
        int sample = Math.min(text.length(), 8000);
        int control = 0;
        for (int i = 0; i < sample; i++) {
            char c = text.charAt(i);
            if (c == '\n' || c == '\t' || c == '\r') {
                continue;
            }
            if (Character.isISOControl(c)) {
                control++;
            }
        }
        return control > sample * 0.02;
    }
}
