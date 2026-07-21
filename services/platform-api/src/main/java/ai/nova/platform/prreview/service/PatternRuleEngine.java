package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;

final class PatternRuleEngine {

    private PatternRuleEngine() {
    }

    record Rule(
            String code,
            Pattern pattern,
            ReviewSeverity severity,
            String title,
            String description,
            String recommendation) {
    }

    static List<FindingDraft> apply(ReviewCategory category, String content, List<Rule> rules) {
        List<FindingDraft> findings = new ArrayList<>();
        if (content == null || content.isBlank() || rules == null) {
            return findings;
        }
        for (Rule rule : rules) {
            Matcher matcher = rule.pattern().matcher(content);
            if (matcher.find()) {
                String excerpt = truncate(matcher.group(), 240);
                findings.add(FindingDraft.of(
                        category,
                        rule.severity(),
                        rule.code(),
                        rule.title(),
                        rule.description(),
                        rule.recommendation(),
                        excerpt));
            }
        }
        return findings;
    }

    static String truncate(String value, int max) {
        if (value == null) {
            return null;
        }
        String normalized = value.replaceAll("\\s+", " ").trim();
        return normalized.length() <= max ? normalized : normalized.substring(0, max) + "...";
    }
}
