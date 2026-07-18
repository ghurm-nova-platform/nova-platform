package ai.nova.platform.patch.service;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.web.error.ApiException;

@Service
public class PatchJsonParser {

    private final ObjectMapper objectMapper;

    public PatchJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedPatchOutput parse(String rawText) {
        String json = extractJson(rawText);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw invalid("Patch output must be a JSON object");
            }
            String summary = text(root, "summary");
            if (summary == null || summary.isBlank()) {
                summary = "Generated patch";
            }
            Integer filesChanged = number(root, "filesChanged");
            Integer insertions = number(root, "insertions");
            Integer deletions = number(root, "deletions");
            String patch = text(root, "patch");
            if (patch == null || patch.isBlank()) {
                patch = text(root, "diff");
            }
            PatchStatus status = statusValue(root);
            return new ParsedPatchOutput(summary.trim(), filesChanged, insertions, deletions, patch, status);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("Failed to parse patch JSON: " + ex.getMessage());
        }
    }

    private static PatchStatus statusValue(JsonNode root) {
        String raw = text(root, "status");
        if (raw == null || raw.isBlank()) {
            return PatchStatus.VALID;
        }
        String normalized = raw.trim().toUpperCase(Locale.ROOT).replace('-', '_').replace(' ', '_');
        try {
            return Enum.valueOf(PatchStatus.class, normalized);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PATCH_UNKNOWN_STATUS", "Unknown patch status: " + raw);
        }
    }

    private static Integer number(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isNumber()) {
            throw invalid(field + " must be a number");
        }
        return node.asInt();
    }

    private static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw invalid("Patch output is empty");
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
            throw invalid("Patch output does not contain a JSON object");
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

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "PATCH_INVALID_JSON", message);
    }
}
