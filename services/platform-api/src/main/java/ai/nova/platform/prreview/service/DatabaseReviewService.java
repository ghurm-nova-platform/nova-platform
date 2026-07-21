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
public class DatabaseReviewService {

    private static final List<Rule> RULES = List.of(
            new Rule(
                    "DB_MISSING_FK",
                    Pattern.compile("(?i)CREATE\\s+TABLE[\\s\\S]{0,800}(?!.*REFERENCES)"),
                    ReviewSeverity.WARNING,
                    "Possible missing foreign key",
                    "DDL creates a table without an apparent REFERENCES clause in the nearby excerpt.",
                    "Add foreign keys for related columns to protect referential integrity."),
            new Rule(
                    "DB_MISSING_INDEX",
                    Pattern.compile("(?i)WHERE\\s+\\w+\\s*=|JOIN\\s+\\w+\\s+ON"),
                    ReviewSeverity.SUGGESTION,
                    "Query without nearby index hint",
                    "Diff contains filter/join predicates; verify supporting indexes exist.",
                    "Add indexes for frequently filtered and joined columns."),
            new Rule(
                    "DB_WRONG_CASCADE",
                    Pattern.compile("(?i)ON\\s+DELETE\\s+CASCADE"),
                    ReviewSeverity.WARNING,
                    "ON DELETE CASCADE usage",
                    "Diff uses ON DELETE CASCADE which can wipe related rows unexpectedly.",
                    "Prefer RESTRICT/SET NULL unless cascade deletion is an explicit domain decision."),
            new Rule(
                    "DB_MISSING_OPTIMISTIC_LOCK",
                    Pattern.compile("(?i)@Entity[\\s\\S]{0,400}(?!.*@Version)"),
                    ReviewSeverity.SUGGESTION,
                    "Entity without @Version",
                    "JPA entity changes appear without optimistic locking (@Version).",
                    "Add @Version for concurrent update safety on mutable aggregates."),
            new Rule(
                    "DB_DROP_WITHOUT_BACKUP",
                    Pattern.compile("(?i)DROP\\s+(TABLE|COLUMN|INDEX)"),
                    ReviewSeverity.ERROR,
                    "Destructive DDL detected",
                    "Diff contains DROP TABLE/COLUMN/INDEX which is high-risk for production.",
                    "Document a rollback plan and prefer expandable/contractable migrations."),
            new Rule(
                    "DB_DUPLICATE_INDEX_HINT",
                    Pattern.compile("(?i)CREATE\\s+(UNIQUE\\s+)?INDEX[\\s\\S]{0,120}CREATE\\s+(UNIQUE\\s+)?INDEX"),
                    ReviewSeverity.INFO,
                    "Multiple index creations close together",
                    "Diff creates multiple indexes in close proximity; check for duplicates.",
                    "Consolidate overlapping indexes before applying."));

    public List<FindingDraft> analyze(String content) {
        return analyze(ReviewContext.of(content));
    }

    public List<FindingDraft> analyze(ReviewContext context) {
        List<FindingDraft> findings = new ArrayList<>(
                PatternRuleEngine.apply(ReviewCategory.Database, context.safeContent(), RULES));
        String joinedFiles = String.join("\n", context.changedFiles()).toLowerCase(Locale.ROOT);
        if ((joinedFiles.contains("migration") || joinedFiles.contains(".sql"))
                && !context.safeContent().toLowerCase(Locale.ROOT).contains("rollback")) {
            findings.add(FindingDraft.of(
                    ReviewCategory.Database,
                    ReviewSeverity.SUGGESTION,
                    "DB_MIGRATION_NO_ROLLBACK_NOTE",
                    "Migration without rollback notes",
                    "Changed migration/SQL files do not mention rollback handling in the submitted content.",
                    "Document rollback or expand/contract steps in the migration or PR description.",
                    null));
        }
        return findings;
    }
}
