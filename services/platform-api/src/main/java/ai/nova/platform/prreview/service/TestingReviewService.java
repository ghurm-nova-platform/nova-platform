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
public class TestingReviewService {

    private static final List<Rule> RULES = List.of(
            new Rule(
                    "TEST_IGNORED",
                    Pattern.compile("@Ignore|@Disabled|xit\\(|xdescribe\\("),
                    ReviewSeverity.WARNING,
                    "Ignored or disabled tests",
                    "Diff disables tests which may hide regressions.",
                    "Re-enable tests or document why they remain skipped."),
            new Rule(
                    "TEST_EMPTY_ASSERT",
                    Pattern.compile("(?i)@Test[\\s\\S]{0,200}\\{\\s*\\}"),
                    ReviewSeverity.ERROR,
                    "Empty test method",
                    "A @Test method appears empty in the submitted excerpt.",
                    "Add meaningful assertions or remove the placeholder test."));

    public List<FindingDraft> analyze(String content) {
        return analyze(ReviewContext.of(content));
    }

    public List<FindingDraft> analyze(ReviewContext context) {
        List<FindingDraft> findings = new ArrayList<>(
                PatternRuleEngine.apply(ReviewCategory.Testing, context.safeContent(), RULES));
        List<String> files = context.changedFiles();
        if (files.isEmpty()) {
            return findings;
        }
        boolean productionChanged = files.stream().anyMatch(this::isProductionCode);
        boolean testChanged = files.stream().anyMatch(this::isTestCode);
        boolean controllerChanged = files.stream()
                .anyMatch(f -> f.toLowerCase(Locale.ROOT).contains("controller"));
        boolean controllerTestChanged = files.stream()
                .anyMatch(f -> {
                    String lower = f.toLowerCase(Locale.ROOT);
                    return lower.contains("controller") && isTestCode(f);
                });

        if (productionChanged && !testChanged) {
            findings.add(FindingDraft.of(
                    ReviewCategory.Testing,
                    ReviewSeverity.WARNING,
                    "TEST_MISSING_UNIT",
                    "Production code changed without tests",
                    "Changed production files have no accompanying test file changes.",
                    "Add or update unit/integration tests covering the changed behavior.",
                    null));
        }
        if (controllerChanged && !controllerTestChanged) {
            findings.add(FindingDraft.of(
                    ReviewCategory.Testing,
                    ReviewSeverity.WARNING,
                    "TEST_MISSING_CONTROLLER",
                    "Controller changed without controller tests",
                    "Controller files changed without matching controller test updates.",
                    "Add MockMvc/WebTestClient coverage for new or changed endpoints.",
                    null));
        }
        return findings;
    }

    private boolean isProductionCode(String path) {
        String lower = path.toLowerCase(Locale.ROOT).replace('\\', '/');
        if (isTestCode(path)) {
            return false;
        }
        return lower.contains("/main/") || lower.endsWith(".java") || lower.endsWith(".ts");
    }

    private boolean isTestCode(String path) {
        String lower = path.toLowerCase(Locale.ROOT).replace('\\', '/');
        return lower.contains("/test/")
                || lower.contains(".spec.")
                || lower.endsWith("test.java")
                || lower.endsWith("tests.java")
                || lower.contains("__tests__");
    }
}
