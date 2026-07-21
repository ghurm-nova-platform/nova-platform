package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;

@Service
public class QualityReviewService {

    private static final int LONG_METHOD_LINE_THRESHOLD = 80;
    private static final int TODO_DENSITY_THRESHOLD = 5;
    private static final Pattern METHOD_START =
            Pattern.compile("(?m)^\\s*(public|protected|private|static|final|synchronized|native|abstract|default|\\s)+"
                    + "[\\w<>\\[\\].,?\\s]+\\s+\\w+\\s*\\([^;]*\\)\\s*\\{");
    private static final Pattern EMPTY_CATCH =
            Pattern.compile("catch\\s*\\(\\s*Exception\\s+\\w+\\s*\\)\\s*\\{\\s*\\}");
    private static final Pattern SYSTEM_OUT =
            Pattern.compile("System\\.out\\.println");
    private static final Pattern TODO_FIXME =
            Pattern.compile("(?i)\\b(TODO|FIXME)\\b");

    public List<FindingDraft> analyze(ReviewContext context) {
        return analyze(context == null ? null : context.safeContent());
    }

    public List<FindingDraft> analyze(String content) {
        List<FindingDraft> findings = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return findings;
        }

        detectLongMethods(content, findings);
        detectDuplicatedLines(content, findings);

        if (EMPTY_CATCH.matcher(content).find()) {
            findings.add(new FindingDraft(
                    ReviewCategory.CodeQuality,
                    ReviewSeverity.ERROR,
                    "Empty catch (Exception) block",
                    "Diff contains catch (Exception e) {} which swallows failures silently.",
                    "Handle or rethrow exceptions; at minimum log with context."));
        }

        if (SYSTEM_OUT.matcher(content).find()) {
            findings.add(new FindingDraft(
                    ReviewCategory.CodeQuality,
                    ReviewSeverity.SUGGESTION,
                    "System.out.println usage",
                    "Diff uses System.out.println instead of a structured logger.",
                    "Replace console printing with the project logging framework."));
        }

        int todoCount = 0;
        Matcher todoMatcher = TODO_FIXME.matcher(content);
        while (todoMatcher.find()) {
            todoCount++;
        }
        if (todoCount >= TODO_DENSITY_THRESHOLD) {
            findings.add(new FindingDraft(
                    ReviewCategory.CodeQuality,
                    ReviewSeverity.WARNING,
                    "High TODO/FIXME density",
                    "Diff contains " + todoCount + " TODO/FIXME markers.",
                    "Resolve or track outstanding TODOs before merge."));
        }

        return findings;
    }

    private void detectLongMethods(String content, List<FindingDraft> findings) {
        String[] lines = content.split("\\R", -1);
        Matcher matcher = METHOD_START.matcher(content);
        List<Integer> starts = new ArrayList<>();
        while (matcher.find()) {
            starts.add(lineNumberAt(content, matcher.start()));
        }
        for (int i = 0; i < starts.size(); i++) {
            int startLine = starts.get(i);
            int endLine = i + 1 < starts.size() ? starts.get(i + 1) : lines.length;
            int length = Math.max(0, endLine - startLine);
            if (length >= LONG_METHOD_LINE_THRESHOLD) {
                findings.add(new FindingDraft(
                        ReviewCategory.CodeQuality,
                        ReviewSeverity.WARNING,
                        "Long method in diff excerpt",
                        "A method spanning approximately " + length + " lines exceeds the " + LONG_METHOD_LINE_THRESHOLD
                                + "-line threshold.",
                        "Extract smaller helpers to improve readability and testability.",
                        null,
                        startLine,
                        List.of(),
                        List.of()));
                break;
            }
        }
    }

    private void detectDuplicatedLines(String content, List<FindingDraft> findings) {
        Map<String, Integer> counts = new HashMap<>();
        for (String raw : content.split("\\R")) {
            String line = raw.trim();
            if (line.length() < 40) {
                continue;
            }
            String key = line.toLowerCase(Locale.ROOT);
            counts.merge(key, 1, Integer::sum);
        }
        boolean duplicated = counts.values().stream().anyMatch(count -> count >= 3);
        if (duplicated) {
            findings.add(new FindingDraft(
                    ReviewCategory.CodeQuality,
                    ReviewSeverity.SUGGESTION,
                    "Duplicated identical lines",
                    "Diff contains identical non-trivial lines repeated multiple times.",
                    "Extract shared constants or helpers to reduce duplication."));
        }
    }

    private int lineNumberAt(String content, int offset) {
        int line = 1;
        for (int i = 0; i < offset && i < content.length(); i++) {
            if (content.charAt(i) == '\n') {
                line++;
            }
        }
        return line;
    }
}
