package ai.nova.platform.coding.service;

import java.util.Arrays;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ai.nova.platform.coding.config.CodingProperties;
import ai.nova.platform.coding.dto.CodingDtos.CodingPromptContext;
import ai.nova.platform.coding.dto.CodingDtos.CodingTask;
import ai.nova.platform.coding.dto.CodingDtos.DependencySummary;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;

@Service
public class CodingPromptBuilder {

    private final CodingProperties properties;

    public CodingPromptBuilder(CodingProperties properties) {
        this.properties = properties;
    }

    public String buildSystemPrompt() {
        return properties.getDefaultSystemPrompt().trim();
    }

    public String buildUserPrompt(CodingPromptContext context) {
        CodingTask task = context.task();
        String languages = Arrays.stream(ArtifactLanguage.values())
                .map(Enum::name)
                .collect(Collectors.joining(", "));
        String types = Arrays.stream(ArtifactType.values()).map(Enum::name).collect(Collectors.joining(", "));

        StringBuilder deps = new StringBuilder();
        if (context.dependencies() == null || context.dependencies().isEmpty()) {
            deps.append("(none)\n");
        } else {
            for (DependencySummary dep : context.dependencies()) {
                deps.append("- ")
                        .append(dep.taskKey())
                        .append(" (")
                        .append(dep.displayName())
                        .append(") status=")
                        .append(dep.status())
                        .append('\n');
                if (dep.outputPreview() != null && !dep.outputPreview().isBlank()) {
                    deps.append("  outputPreview=")
                            .append(truncate(dep.outputPreview(), 800))
                            .append('\n');
                }
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
                - type=%s
                - status=%s
                - modelReference=%s
                - inputJson=%s

                Upstream dependencies:
                %s
                Available languages:
                %s

                Artifact types:
                %s

                Coding conventions:
                %s

                Organization settings:
                %s
                Project settings:
                %s
                Output schema (JSON only):
                {
                  "summary": "short summary",
                  "artifacts": [
                    {
                      "type": "SOURCE_FILE|PATCH|TEST|DOCUMENTATION|CONFIGURATION|SQL_MIGRATION|README",
                      "language": "JAVA|KOTLIN|TYPESCRIPT|...",
                      "path": "relative/path/File.ext",
                      "filename": "File.ext",
                      "content": "file contents"
                    }
                  ]
                }

                Rules:
                - Return JSON only.
                - Use relative paths only (no absolute paths, no .. traversal).
                - Never include binary content.
                - Do not execute tools, shell, git, docker, browser, MCP, or terminal.
                """
                .formatted(
                        nullToEmpty(context.objective()),
                        task.id(),
                        nullToEmpty(task.taskKey()),
                        nullToEmpty(task.displayName()),
                        nullToEmpty(task.description()),
                        nullToEmpty(task.taskType()),
                        nullToEmpty(task.status()),
                        nullToEmpty(task.modelReference()),
                        truncate(nullToEmpty(task.inputJson()), 4000),
                        deps,
                        languages,
                        types,
                        properties.getCodingConventions().trim(),
                        orgSettings,
                        projectSettings)
                .trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private static String truncate(String value, int max) {
        if (value == null) {
            return "";
        }
        String normalized = value.replace('\r', ' ').replace('\n', ' ');
        return normalized.length() <= max ? normalized : normalized.substring(0, max);
    }
}
