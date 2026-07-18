package ai.nova.platform.patch.service;

import org.springframework.stereotype.Service;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.patch.config.PatchProperties;
import ai.nova.platform.patch.dto.PatchDtos.PatchPromptContext;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFinding;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTest;

@Service
public class PatchPromptBuilder {

    private final PatchProperties properties;

    public PatchPromptBuilder(PatchProperties properties) {
        this.properties = properties;
    }

    public String buildSystemPrompt() {
        return properties.getDefaultSystemPrompt().trim();
    }

    public String buildUserPrompt(PatchPromptContext context) {
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

        StringBuilder tests = new StringBuilder();
        if (context.generatedTests() == null || context.generatedTests().isEmpty()) {
            tests.append("(none)\n");
        } else {
            for (GeneratedTest test : context.generatedTests()) {
                tests.append("- ")
                        .append(test.type())
                        .append('/')
                        .append(test.priority())
                        .append(": ")
                        .append(test.title())
                        .append(" — ")
                        .append(test.description())
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
                Task:
                - id=%s
                - key=%s
                - name=%s
                - description=%s
                - objective=%s

                Generated artifacts:
                %s
                Review:
                - approved=%s
                - score=%s
                - summary=%s
                Review findings:
                %s
                Testing:
                - coverageEstimate=%s
                - summary=%s
                Generated tests:
                %s
                Repository conventions:
                %s

                Unified Diff specification:
                - Use Git unified diff format.
                - Each file needs --- a/<path> and +++ b/<path> (or /dev/null for add/delete).
                - Include one or more @@ -old,count +new,count @@ hunks.
                - Lines start with space (context), + (add), or - (remove).

                Organization settings:
                %s
                Project settings:
                %s
                Output schema (JSON only):
                {
                  "summary": "short summary",
                  "filesChanged": 1,
                  "insertions": 1,
                  "deletions": 0,
                  "patch": "--- a/path\\n+++ b/path\\n@@ -1,1 +1,2 @@\\n context\\n+added\\n",
                  "status": "VALID"
                }

                Rules:
                - Return JSON only.
                - stats must match the patch text.
                - Never execute git, shell, apply, commit, push, or merge.
                - Never modify repositories. Generate patch text only.
                """
                .formatted(
                        context.taskId(),
                        nullToEmpty(context.taskKey()),
                        nullToEmpty(context.displayName()),
                        nullToEmpty(context.description()),
                        nullToEmpty(context.objective()),
                        artifacts,
                        context.reviewApproved(),
                        context.reviewScore() == null ? "n/a" : context.reviewScore(),
                        nullToEmpty(context.reviewSummary()),
                        findings,
                        context.coverageEstimate() == null ? "n/a" : context.coverageEstimate(),
                        nullToEmpty(context.testingSummary()),
                        tests,
                        properties.getRepositoryConventions().trim(),
                        orgSettings,
                        projectSettings)
                .trim();
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
