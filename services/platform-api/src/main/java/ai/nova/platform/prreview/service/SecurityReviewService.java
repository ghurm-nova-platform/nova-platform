package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;

@Service
public class SecurityReviewService {

    private static final List<Rule> RULES = List.of(
            new Rule(
                    Pattern.compile("(?i)password\\s*="),
                    ReviewSeverity.BLOCKER,
                    "Hardcoded password assignment",
                    "Diff contains a password= assignment which may leak credentials.",
                    "Remove hardcoded passwords and load secrets from a secure secret store."),
            new Rule(
                    Pattern.compile("(?i)api[_-]?key\\s*="),
                    ReviewSeverity.BLOCKER,
                    "Hardcoded API key assignment",
                    "Diff contains an api_key= (or similar) assignment which may leak credentials.",
                    "Remove hardcoded API keys and inject them from configuration or a vault."),
            new Rule(
                    Pattern.compile("(?i)secret\\s*="),
                    ReviewSeverity.ERROR,
                    "Hardcoded secret assignment",
                    "Diff contains a secret= assignment which may leak sensitive material.",
                    "Externalize secrets and rotate any values that may have been committed."),
            new Rule(
                    Pattern.compile("(?i)Bearer\\s+[A-Za-z0-9._\\-]+"),
                    ReviewSeverity.ERROR,
                    "Bearer token in source",
                    "Diff appears to embed a Bearer token literal.",
                    "Do not commit bearer tokens; use short-lived credentials from a secure store."),
            new Rule(
                    Pattern.compile("(?i)SELECT[\\s\\S]{0,80}\\+[\\s\\S]{0,40}request"),
                    ReviewSeverity.BLOCKER,
                    "Possible SQL string concatenation",
                    "Diff concatenates SQL with request-derived values, which is a SQL injection risk.",
                    "Use parameterized queries or a typed query API."),
            new Rule(
                    Pattern.compile("innerHTML"),
                    ReviewSeverity.WARNING,
                    "innerHTML usage",
                    "Diff uses innerHTML which can enable XSS if fed untrusted input.",
                    "Prefer textContent or a sanitized HTML renderer."),
            new Rule(
                    Pattern.compile("Runtime\\.exec"),
                    ReviewSeverity.ERROR,
                    "Runtime.exec usage",
                    "Diff calls Runtime.exec which can enable command injection.",
                    "Avoid shelling out; if required, use an allowlisted ProcessBuilder with no user input."),
            new Rule(
                    Pattern.compile("ObjectInputStream"),
                    ReviewSeverity.ERROR,
                    "Java deserialization via ObjectInputStream",
                    "Diff uses ObjectInputStream which is a common deserialization gadget risk.",
                    "Prefer safe formats such as JSON with an allowlisted schema."),
            new Rule(
                    Pattern.compile("\\.\\./"),
                    ReviewSeverity.WARNING,
                    "Path traversal pattern",
                    "Diff contains '../' which may indicate path traversal handling issues.",
                    "Normalize and validate paths against an allowlisted root before file access."));

    public List<FindingDraft> analyze(ReviewContext context) {
        return analyze(context == null ? null : context.safeContent());
    }

    public List<FindingDraft> analyze(String content) {
        List<FindingDraft> findings = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return findings;
        }
        for (Rule rule : RULES) {
            if (rule.pattern.matcher(content).find()) {
                findings.add(new FindingDraft(
                        ReviewCategory.Security,
                        rule.severity,
                        rule.title,
                        rule.description,
                        rule.recommendation));
            }
        }
        return findings;
    }

    private record Rule(
            Pattern pattern, ReviewSeverity severity, String title, String description, String recommendation) {
    }
}
