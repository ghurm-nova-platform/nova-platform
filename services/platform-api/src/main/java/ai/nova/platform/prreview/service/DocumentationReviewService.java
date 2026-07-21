package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;
import ai.nova.platform.prreview.service.PatternRuleEngine.Rule;

@Service
public class DocumentationReviewService {

    private static final List<Rule> RULES = List.of(
            new Rule(
                    "DOC_MISSING_ADR_HINT",
                    Pattern.compile("(?i)(breaking\\s+change|architecture\\s+decision|new\\s+module)"),
                    ReviewSeverity.SUGGESTION,
                    "Possible missing ADR",
                    "Diff mentions architectural/breaking change language without ADR references.",
                    "Capture the decision in an ADR and link it from the PR."),
            new Rule(
                    "DOC_MISSING_API_DOCS",
                    Pattern.compile("(?i)@RestController|@RequestMapping|/api/"),
                    ReviewSeverity.SUGGESTION,
                    "API surface changed — verify docs",
                    "Diff touches REST API surface area; API documentation may need updates.",
                    "Update OpenAPI/README sections that describe the changed endpoints."));

    public List<FindingDraft> analyze(String content) {
        return analyze(ReviewContext.of(content));
    }

    public List<FindingDraft> analyze(ReviewContext context) {
        List<FindingDraft> findings = new ArrayList<>(
                PatternRuleEngine.apply(ReviewCategory.Documentation, context.safeContent(), RULES));
        List<String> files = context.changedFiles();
        boolean codeChanged = files.stream().anyMatch(this::looksLikeCode);
        boolean readmeChanged = files.stream().anyMatch(f -> f.toLowerCase(Locale.ROOT).contains("readme"));
        boolean migrationChanged =
                files.stream().anyMatch(f -> f.toLowerCase(Locale.ROOT).matches(".*migration.*|.*\\.sql"));
        boolean docsChanged = files.stream()
                .anyMatch(f -> f.toLowerCase(Locale.ROOT).contains("docs/")
                        || f.toLowerCase(Locale.ROOT).endsWith(".md"));

        if (codeChanged && !readmeChanged && !files.isEmpty()) {
            findings.add(FindingDraft.of(
                    ReviewCategory.Documentation,
                    ReviewSeverity.SUGGESTION,
                    "DOC_MISSING_README",
                    "Code changed without README update",
                    "Changed files include code but no README update was listed.",
                    "Update README when behavior, setup, or configuration changes.",
                    null));
        }
        if (migrationChanged && !docsChanged) {
            findings.add(FindingDraft.of(
                    ReviewCategory.Documentation,
                    ReviewSeverity.WARNING,
                    "DOC_MISSING_MIGRATION_DOCS",
                    "Migration without documentation update",
                    "SQL/migration files changed without accompanying docs updates.",
                    "Document migration impact, downtime, and rollback in docs or the PR body.",
                    null));
        }
        return findings;
    }

    private boolean looksLikeCode(String path) {
        String lower = path.toLowerCase(Locale.ROOT);
        return lower.endsWith(".java")
                || lower.endsWith(".ts")
                || lower.endsWith(".tsx")
                || lower.endsWith(".js")
                || lower.endsWith(".py")
                || lower.endsWith(".go");
    }
}
