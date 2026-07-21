package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;

@Service
public class PerformanceReviewService {

    private static final Pattern FIND_ALL_IN_LOOP = Pattern.compile(
            "(?is)(for\\s*\\(|while\\s*\\(|\\.forEach\\s*\\()[\\s\\S]{0,200}findAll\\s*\\(");
    private static final Pattern SELECT_STAR_NO_LIMIT =
            Pattern.compile("(?i)SELECT\\s+\\*[\\s\\S]{0,120}(?!LIMIT)");
    private static final Pattern SELECT_STAR = Pattern.compile("(?i)SELECT\\s+\\*");
    private static final Pattern SELECT_WITH_LIMIT = Pattern.compile("(?i)LIMIT\\s+\\d+");
    private static final Pattern LARGE_BYTE_ARRAY =
            Pattern.compile("new\\s+byte\\s*\\[\\s*([1-9]\\d{6,}|\\d{8,})\\s*\\]");
    private static final Pattern NESTED_FOR_DB = Pattern.compile(
            "(?is)for\\s*\\([^)]*\\)\\s*\\{[\\s\\S]{0,300}for\\s*\\([^)]*\\)\\s*\\{[\\s\\S]{0,300}"
                    + "(repository\\.|jdbc|findBy|save\\(|query\\()");

    public List<FindingDraft> analyze(ReviewContext context) {
        return analyze(context == null ? null : context.safeContent());
    }

    public List<FindingDraft> analyze(String content) {
        List<FindingDraft> findings = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return findings;
        }

        if (FIND_ALL_IN_LOOP.matcher(content).find()) {
            findings.add(new FindingDraft(
                    ReviewCategory.Performance,
                    ReviewSeverity.ERROR,
                    "findAll() inside a loop",
                    "Diff appears to call findAll() inside a loop, which can cause N+1 or unbounded loads.",
                    "Batch-load entities outside the loop or use a targeted query with pagination."));
        }

        if (SELECT_STAR.matcher(content).find() && !SELECT_WITH_LIMIT.matcher(content).find()) {
            findings.add(new FindingDraft(
                    ReviewCategory.Performance,
                    ReviewSeverity.WARNING,
                    "SELECT * without LIMIT",
                    "Diff contains SELECT * without an apparent LIMIT, which may load unbounded result sets.",
                    "Select required columns and apply LIMIT/OFFSET or keyset pagination."));
        } else if (SELECT_STAR_NO_LIMIT.matcher(content).find() && SELECT_STAR.matcher(content).find()) {
            // covered above
        }

        if (LARGE_BYTE_ARRAY.matcher(content).find()) {
            findings.add(new FindingDraft(
                    ReviewCategory.Performance,
                    ReviewSeverity.WARNING,
                    "Large byte array allocation",
                    "Diff allocates a very large byte[] which may pressure heap memory.",
                    "Stream data in chunks or bound the allocation size."));
        }

        if (NESTED_FOR_DB.matcher(content).find()) {
            findings.add(new FindingDraft(
                    ReviewCategory.Performance,
                    ReviewSeverity.ERROR,
                    "Nested loops with database calls",
                    "Diff appears to nest loops that perform repository/DB operations.",
                    "Prefetch related data or redesign the query to avoid nested database round-trips."));
        }

        return findings;
    }
}
