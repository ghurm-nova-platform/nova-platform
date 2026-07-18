package ai.nova.platform.testing.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.testing.dto.TestingDtos.GeneratedTestDraft;
import ai.nova.platform.testing.dto.TestingDtos.ParsedTestingOutput;
import ai.nova.platform.testing.dto.TestingDtos.TestCaseDraft;
import ai.nova.platform.testing.entity.TestPriority;
import ai.nova.platform.testing.entity.TestType;
import ai.nova.platform.web.error.ApiException;

@Service
public class TestingJsonParser {

    private final ObjectMapper objectMapper;

    public TestingJsonParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ParsedTestingOutput parse(String rawText) {
        String json = extractJson(rawText);
        try {
            JsonNode root = objectMapper.readTree(json);
            if (!root.isObject()) {
                throw invalid("Testing output must be a JSON object");
            }
            String summary = text(root, "summary");
            if (summary == null || summary.isBlank()) {
                summary = "Test plan generated";
            }
            Integer coverage = null;
            if (root.path("coverageEstimate").isNumber()) {
                coverage = root.path("coverageEstimate").asInt();
            } else if (root.path("coverage").isNumber()) {
                coverage = root.path("coverage").asInt();
            }

            List<GeneratedTestDraft> tests = new ArrayList<>();
            JsonNode testsNode = root.path("generatedTests");
            if (testsNode.isArray()) {
                for (JsonNode node : testsNode) {
                    tests.add(parseTest(node));
                }
            } else if (!testsNode.isMissingNode() && !testsNode.isNull()) {
                throw invalid("generatedTests must be an array");
            }
            return new ParsedTestingOutput(summary.trim(), coverage, List.copyOf(tests));
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalid("Failed to parse testing JSON: " + ex.getMessage());
        }
    }

    private GeneratedTestDraft parseTest(JsonNode node) {
        TestType type = enumValue(node, "type", TestType.class);
        TestPriority priority = enumValue(node, "priority", TestPriority.class);
        String title = text(node, "title");
        String description = text(node, "description");
        String artifactPath = text(node, "artifactPath");
        if (artifactPath == null) {
            artifactPath = text(node, "path");
        }
        List<TestCaseDraft> cases = new ArrayList<>();
        JsonNode casesNode = node.path("cases");
        if (casesNode.isArray()) {
            for (JsonNode caseNode : casesNode) {
                cases.add(parseCase(caseNode, priority));
            }
        }
        return new GeneratedTestDraft(type, priority, title, description, artifactPath, List.copyOf(cases));
    }

    private TestCaseDraft parseCase(JsonNode node, TestPriority fallbackPriority) {
        String name = text(node, "name");
        if (name == null || name.isBlank()) {
            name = text(node, "title");
        }
        String steps = text(node, "steps");
        String expected = text(node, "expectedResult");
        if (expected == null) {
            expected = text(node, "expected");
        }
        TestPriority priority = fallbackPriority;
        if (node.hasNonNull("priority") && !node.get("priority").asText().isBlank()) {
            priority = enumValue(node, "priority", TestPriority.class);
        }
        return new TestCaseDraft(name, steps, expected, priority);
    }

    private static String extractJson(String rawText) {
        if (rawText == null || rawText.isBlank()) {
            throw invalid("Testing output is empty");
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
            throw invalid("Testing output does not contain a JSON object");
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
        if ("EDGECASE".equals(normalized)) {
            normalized = "EDGE_CASE";
        }
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException ex) {
            if (type == TestType.class) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "TESTING_UNKNOWN_TYPE", "Unknown test type: " + raw);
            }
            if (type == TestPriority.class) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST, "TESTING_UNKNOWN_PRIORITY", "Unknown priority: " + raw);
            }
            throw invalid("Invalid value for " + field + ": " + raw);
        }
    }

    private static ApiException invalid(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "TESTING_INVALID_JSON", message);
    }
}
