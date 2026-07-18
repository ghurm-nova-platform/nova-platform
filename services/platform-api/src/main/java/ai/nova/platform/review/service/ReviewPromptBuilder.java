package ai.nova.platform.review.service;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.review.config.ReviewProperties;
import ai.nova.platform.review.dto.ReviewDtos.ReviewPromptContext;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;

@Service
public class ReviewPromptBuilder {

    private final ReviewProperties properties;

    public ReviewPromptBuilder(ReviewProperties properties) {
        this.properties = properties;
    }

    public String buildSystemPrompt() {
        return properties.getDefaultSystemPrompt().trim();
    }

    public String buildUserPrompt(ReviewPromptContext context) {
        String severities =
                Arrays.stream(ReviewSeverity.values()).map(Enum::name).collect(Collectors.joining(", "));
        String categories =
                Arrays.stream(ReviewCategory.values()).map(Enum::name).collect(Collectors.joining(", "));

        StringBuilder artifacts = new StringBuilder();
        int budget = properties.getMaxArtifactCharsInPrompt();
        int used = 0;
        for (GeneratedArtifactResponse artifact : context.artifacts()) {
            String block = """
                    ---
                    path=%s
                    filename=%s
                    language=%s
                    type=%s
                    sha256=%s
                    content:
                    %s
                    """
                    .formatted(
                            artifact.path(),
                            artifact.filename(),
                            artifact.language(),
                            artifact.artifactType(),
                            artifact.sha256(),
                            artifact.content());
            if (used + block.length() > budget) {
                artifacts.append("---\n(truncated remaining artifacts due to prompt budget)\n");
                break;
            }
            artifacts.append(block);
            used += block.length();
        }
        if (artifacts.isEmpty()) {
            artifacts.append("(no artifacts)\n");
        }

        StringBuilder orgSettings = new StringBuilder();
        context.organizationSettings()
                .forEach((k, v) -> orgSettings.append("- ").append(k).append("=").append(v).append('\n'));
        if (orgSettings.isEmpty()) {
            orgSettings.append("(none)\n");
        }
        StringBuilder projectSettings = new StringBuilder();
        context.projectSettings()
                .forEach((k, v) -> projectSettings.append("- ").append(k).append("=").append(v).append('\n'));
        if (projectSettings.isEmpty()) {
            projectSettings.append("(none)\n");
        }

        return """
                Objective:
                %s

                Task:
                - id=%s
                - key=%s
                - name=%s
                - description=%s

                Generated artifacts:
                %s
                Available severities:
                %s

                Available categories:
                %s

                Coding conventions:
                %s

                Architecture rules:
                %s

                Security rules:
                %s

                Organization settings:
                %s
                Project settings:
                %s
                Output schema (JSON only):
                {
                  "summary": "overall assessment",
                  "score": 0,
                  "approved": true,
                  "findings": [
                    {
                      "severity": "INFO|LOW|MEDIUM|HIGH|CRITICAL",
                      "category": "CORRECTNESS|SECURITY|...",
                      "title": "short title",
                      "description": "what is wrong",
                      "recommendation": "how to fix",
                      "artifactPath": "optional relative path"
                    }
                  ]
                }

                Rules:
                - Return JSON only.
                - score must be an integer from 0 to 100.
                - Do not modify artifacts, generate patches, execute shell, or edit git.
                - findings may be an empty array when quality is excellent.
                """
                .formatted(
                        nullToEmpty(context.objective()),
                        context.taskId(),
                        nullToEmpty(context.taskKey()),
                        nullToEmpty(context.displayName()),
                        nullToEmpty(context.description()),
                        artifacts,
                        severities,
                        categories,
                        properties.getCodingConventions().trim(),
                        properties.getArchitectureRules().trim(),
                        properties.getSecurityRules().trim(),
                        orgSettings,
                        projectSettings)
                .trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
