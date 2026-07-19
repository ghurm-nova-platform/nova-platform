package ai.nova.platform.repair.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.web.error.ApiException;
@Service
public class RepairJsonParser {

    private final ObjectMapper objectMapper;

    public RepairJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public record ParsedRepairOutput(
            String summary,
            Integer filesChanged,
            Integer insertions,
            Integer deletions,
            String patch,
            PatchStatus status,
            Double confidence,
            String reason,
            List<String> repairedFiles) {
    }

    public ParsedRepairOutput parse(String rawText) {
        String json = extractJson(rawText);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw invalid("Repair output must be a JSON object");
            }
            String summary = text(root, "summary");
            if (summary == null || summary.isBlank()) {
                summary = "Repair patch";
            }
            Integer filesChanged = number(root, "filesChanged");
            Integer insertions = number(root, "insertions");
            Integer deletions = number(root, "deletions");
            String patch = text(root, "patch");
            if (patch == null || patch.isBlank()) {
                patch = text(root, "diff");
            }
            PatchStatus status = statusValue(root);
            Double confidence = doubleValue(root, "confidence");
            String reason = text(root, "reason");
            List<String> repairedFiles = stringList(root, "repairedFiles");
            return new ParsedRepairOutput(
                    summary.trim(),
                    filesChanged,
                    insertions,
                    deletions,
                    patch,
                    status,
                    confidence,
                    reason,
                    repairedFiles);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("Failed to parse repair JSON: " + ex.getMessage());
        }
    }

    public ParsedPatchOutput toPatchOutput(ParsedRepairOutput repair) {
        return new ParsedPatchOutput(
                repair.summary(),
                repair.filesChanged(),
                repair.insertions(),
                repair.deletions(),
                repair.patch(),
                repair.status());
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
            throw new ApiException(HttpStatus.BAD_REQUEST, "REPAIR_UNKNOWN_STATUS", "Unknown repair status: " + raw);
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

    private static Double doubleValue(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return null;
        }
        if (!node.isNumber()) {
            throw invalid(field + " must be a number");
        }
        return node.asDouble();
    }

    private static List<String> stringList(JsonNode root, String field) {
        JsonNode node = root.get(field);
        if (node == null || node.isNull()) {
            return List.of();
        }
        if (!node.isArray()) {
            throw invalid(field + " must be an array");
        }
        List<String> values = new ArrayList<>();
        for (JsonNode item : node) {
            if (item != null && !item.isNull()) {
                values.add(item.asText());
            }
        }
        return List.copyOf(values);
    }

    private static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw invalid("Repair output is empty");
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
            throw invalid("Repair output does not contain a JSON object");
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
        return new ApiException(HttpStatus.BAD_REQUEST, "REPAIR_INVALID_JSON", message);
    }
}
