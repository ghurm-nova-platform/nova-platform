package ai.nova.platform.repair.service;

import org.springframework.stereotype.Service;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.repair.config.RepairProperties;
import ai.nova.platform.repair.service.RepairInputCollector.CollectedInput;

@Service
public class RepairPromptBuilder {

    private final RepairProperties properties;

    public RepairPromptBuilder(RepairProperties properties) {
        this.properties = properties;
    }

    public String buildSystemPrompt() {
        return properties.getDefaultSystemPrompt().trim()
                + "\n\nRepository conventions:\n"
                + properties.getRepositoryConventions().trim()
                + """

                Respond with JSON only:
                {
                  "summary": "...",
                  "reason": "...",
                  "confidence": 0.0-1.0,
                  "filesChanged": 0,
                  "insertions": 0,
                  "deletions": 0,
                  "repairedFiles": ["relative/path"],
                  "patch": "unified diff text",
                  "status": "VALID"
                }
                """;
    }

    public String buildUserPrompt(RepairContext context) {
        StringBuilder failures = new StringBuilder();
        for (CollectedInput input : context.failureInputs()) {
            failures.append("- [")
                    .append(input.sourceType().name())
                    .append("] ")
                    .append(input.detail())
                    .append('\n');
        }
        if (failures.isEmpty()) {
            failures.append("(none)\n");
        }

        StringBuilder artifacts = new StringBuilder();
        for (GeneratedArtifactResponse artifact : context.artifacts()) {
            artifacts.append("---\npath=")
                    .append(artifact.path())
                    .append("\nfilename=")
                    .append(artifact.filename())
                    .append("\nlanguage=")
                    .append(artifact.language())
                    .append("\ncontent:\n")
                    .append(artifact.content())
                    .append("\n");
        }
        if (artifacts.isEmpty()) {
            artifacts.append("(no artifacts)\n");
        }

        PatchResult prior = context.priorPatch();
        return """
                Repair Agent task
                taskId=%s
                taskKey=%s
                displayName=%s
                priorPatchResultId=%s
                priorPatchSummary=%s

                Failure inputs (address these):
                %s
                Prior patch (base for repair; produce a NEW unified diff):
                %s

                Generated artifacts:
                %s
                """
                .formatted(
                        context.task().getId(),
                        context.task().getTaskKey(),
                        context.task().getDisplayName(),
                        prior.id(),
                        prior.summary(),
                        failures,
                        prior.patch(),
                        artifacts);
    }
}
