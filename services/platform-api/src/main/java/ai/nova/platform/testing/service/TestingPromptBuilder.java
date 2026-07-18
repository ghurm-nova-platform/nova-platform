package ai.nova.platform.testing.service;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.testing.config.TestingProperties;
import ai.nova.platform.testing.dto.TestingDtos.TestingPromptContext;
import ai.nova.platform.testing.entity.TestPriority;
import ai.nova.platform.testing.entity.TestType;

@Service
public class TestingPromptBuilder {

    private final TestingProperties properties;

    public TestingPromptBuilder(TestingProperties properties) {
        this.properties = properties;
    }

    public String buildSystemPrompt() {
        return properties.getDefaultSystemPrompt().trim();
    }

    public String buildUserPrompt(TestingPromptContext context) {
        String types = Arrays.stream(TestType.values()).map(Enum::name).collect(Collectors.joining(", "));
        String priorities =
                Arrays.stream(TestPriority.values()).map(Enum::name).collect(Collectors.joining(", "));

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
                    content:
                    %s
                    """
                    .formatted(
                            artifact.path(),
                            artifact.filename(),
                            artifact.language(),
                            artifact.artifactType(),
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

        StringBuilder findings = new StringBuilder();
        if (context.reviewFindings() == null || context.reviewFindings().isEmpty()) {
            findings.append("(none)\n");
        } else {
            for (ReviewFinding finding : context.reviewFindings()) {
                findings.append("- ")
                        .append(finding.severity())
                        .append('/')
                        .append(finding.category())
                        .append(": ")
                        .append(finding.title())
                        .append(" — ")
                        .append(finding.recommendation())
                        .append('\n');
            }
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
                Review findings:
                %s
                Framework guidance:
                %s

                Testing strategy:
                %s

                Available test types:
                %s

                Available priorities:
                %s

                Organization settings:
                %s
                Project settings:
                %s
                Output schema (JSON only):
                {
                  "summary": "short summary",
                  "coverageEstimate": 0,
                  "generatedTests": [
                    {
                      "type": "UNIT|INTEGRATION|API|...",
                      "priority": "LOW|MEDIUM|HIGH|CRITICAL",
                      "title": "short title",
                      "description": "what to verify",
                      "artifactPath": "optional relative path",
                      "cases": [
                        {
                          "name": "case name",
                          "steps": "optional steps",
                          "expectedResult": "optional expected result",
                          "priority": "MEDIUM"
                        }
                      ]
                    }
                  ]
                }

                Rules:
                - Return JSON only.
                - coverageEstimate must be an integer from 0 to 100.
                - Never execute tools, shell, Maven, Gradle, npm, Docker, or code.
                - Never modify repositories. Generate testing assets only.
                """
                .formatted(
                        nullToEmpty(context.objective()),
                        context.taskId(),
                        nullToEmpty(context.taskKey()),
                        nullToEmpty(context.displayName()),
                        nullToEmpty(context.description()),
                        artifacts,
                        findings,
                        properties.getDefaultFramework().trim(),
                        properties.getTestingStrategy().trim(),
                        types,
                        priorities,
                        orgSettings,
                        projectSettings)
                .trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
