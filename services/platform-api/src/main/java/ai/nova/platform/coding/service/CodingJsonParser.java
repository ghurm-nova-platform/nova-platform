package ai.nova.platform.coding.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.ParsedCodingOutput;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.web.error.ApiException;

@Service
public class CodingJsonParser {

    private final ObjectMapper objectMapper;

    public CodingJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedCodingOutput parse(String rawText) {
        String json = extractJson(rawText);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw invalid("Coding output must be a JSON object");
            }
            String summary = text(root, "summary");
            if (summary == null || summary.isBlank()) {
                summary = "Generated artifacts";
            }
            List<GeneratedArtifactDraft> artifacts = new ArrayList<>();
            JsonNode artifactsNode = root.path("artifacts");
            if (artifactsNode.isArray()) {
                for (JsonNode node : artifactsNode) {
                    artifacts.add(parseArtifact(node));
                }
            }
            return new ParsedCodingOutput(summary.trim(), List.copyOf(artifacts));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("Failed to parse coding JSON: " + ex.getMessage());
        }
    }

    private GeneratedArtifactDraft parseArtifact(JsonNode node) {
        ArtifactType type = enumValue(node, "type", ArtifactType.class);
        ArtifactLanguage language = enumValue(node, "language", ArtifactLanguage.class);
        String path = text(node, "path");
        String filename = text(node, "filename");
        String content = text(node, "content");
        if (filename == null || filename.isBlank()) {
            filename = deriveFilename(path);
        }
        return new GeneratedArtifactDraft(type, language, path, filename, content);
    }

    private static String deriveFilename(String path) {
        if (path == null || path.isBlank()) {
            return "artifact.txt";
        }
        String normalized = path.replace('\\', '/');
        int idx = normalized.lastIndexOf('/');
        return idx >= 0 ? normalized.substring(idx + 1) : normalized;
    }

    private static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw invalid("Coding output is empty");
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
            throw invalid("Coding output does not contain a JSON object");
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
        if ("C#".equalsIgnoreCase(raw.trim()) || "CSHARP".equals(normalized) || "C_SHARP".equals(normalized)) {
            normalized = "CSHARP";
        }
        if ("ORACLESQL".equals(normalized) || "ORACLE".equals(normalized)) {
            normalized = "ORACLE_SQL";
        }
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ex) {
            if (type == ArtifactLanguage.class) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "CODING_INVALID_LANGUAGE", "Invalid language: " + raw);
            }
            if (type == ArtifactType.class) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "CODING_INVALID_ARTIFACT_TYPE",
                        "Invalid artifact type: " + raw);
            }
            throw invalid("Invalid value for " + field + ": " + raw);
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "CODING_INVALID_OUTPUT", message);
    }
}
