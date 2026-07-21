package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;

@Service
public class ArchitectureReviewService {

    private static final Pattern CONTROLLER_REPOSITORY =
            Pattern.compile("(?i)controller.*repository|@RestController[\\s\\S]{0,400}Repository");
    private static final Pattern PACKAGE_DECL =
            Pattern.compile("(?m)^\\s*package\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern IMPORT_DECL =
            Pattern.compile("(?m)^\\s*import\\s+([a-zA-Z0-9_.]+)\\s*;");
    private static final Pattern MIXED_CASE_PACKAGE =
            Pattern.compile("package\\s+[a-z0-9_.]*[A-Z][a-zA-Z0-9_.]*\\s*;");

    public List<FindingDraft> analyze(ReviewContext context) {
        return analyze(context == null ? null : context.safeContent());
    }

    public List<FindingDraft> analyze(String content) {
        List<FindingDraft> findings = new ArrayList<>();
        if (content == null || content.isBlank()) {
            return findings;
        }

        if (CONTROLLER_REPOSITORY.matcher(content).find()) {
            findings.add(new FindingDraft(
                    ReviewCategory.Architecture,
                    ReviewSeverity.WARNING,
                    "Possible controller-to-repository layer skip",
                    "Diff suggests a controller may depend directly on a repository, skipping the service layer.",
                    "Route data access through a service layer instead of injecting repositories into controllers."));
        }

        detectCircularImports(content, findings);

        if (MIXED_CASE_PACKAGE.matcher(content).find()) {
            findings.add(new FindingDraft(
                    ReviewCategory.Architecture,
                    ReviewSeverity.SUGGESTION,
                    "Mixed-case package name detected",
                    "Java package declarations should use lowercase segments; mixed-case packages increase packaging chaos.",
                    "Rename packages to lowercase, dot-separated identifiers."));
        }

        return findings;
    }

    private void detectCircularImports(String content, List<FindingDraft> findings) {
        List<String> packages = new ArrayList<>();
        Matcher packageMatcher = PACKAGE_DECL.matcher(content);
        while (packageMatcher.find()) {
            packages.add(packageMatcher.group(1));
        }
        List<String> imports = new ArrayList<>();
        Matcher importMatcher = IMPORT_DECL.matcher(content);
        while (importMatcher.find()) {
            imports.add(importMatcher.group(1));
        }
        if (packages.size() < 2) {
            return;
        }
        Set<String> packageRoots = new HashSet<>();
        for (String pkg : packages) {
            packageRoots.add(rootOf(pkg));
        }
        Set<String> seenPairs = new HashSet<>();
        for (String pkg : packages) {
            String fromRoot = rootOf(pkg);
            for (String imported : imports) {
                String toRoot = rootOf(imported);
                if (fromRoot.equals(toRoot) || !packageRoots.contains(toRoot)) {
                    continue;
                }
                String pairKey = fromRoot.compareTo(toRoot) < 0 ? fromRoot + "|" + toRoot : toRoot + "|" + fromRoot;
                if (!seenPairs.add(pairKey)) {
                    continue;
                }
                boolean reverse = packages.stream().anyMatch(p -> rootOf(p).equals(toRoot))
                        && imports.stream().anyMatch(i -> rootOf(i).equals(fromRoot));
                if (reverse) {
                    findings.add(new FindingDraft(
                            ReviewCategory.Architecture,
                            ReviewSeverity.WARNING,
                            "Possible circular package dependency",
                            "Packages " + fromRoot + " and " + toRoot
                                    + " appear to import each other in the submitted diff.",
                            "Extract shared types into a third package or invert one of the dependencies."));
                    return;
                }
            }
        }
    }

    private String rootOf(String qualified) {
        if (qualified == null || qualified.isBlank()) {
            return "";
        }
        String[] parts = qualified.toLowerCase(Locale.ROOT).split("\\.");
        if (parts.length >= 3) {
            return parts[0] + "." + parts[1] + "." + parts[2];
        }
        return qualified.toLowerCase(Locale.ROOT);
    }
}
