package ai.nova.platform.prreview.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;
import ai.nova.platform.prreview.service.PatternRuleEngine.Rule;

@Service
public class ApiReviewService {

    private static final List<Rule> RULES = List.of(
            new Rule(
                    "API_WRONG_VERB",
                    Pattern.compile("(?i)@(Get|Post|Put|Delete|Patch)Mapping\\(\"[^\"]*(create|update|delete)[^\"]*\"\\)"),
                    ReviewSeverity.WARNING,
                    "Possibly incorrect HTTP verb",
                    "Mapping path suggests a mutating action that may not match the HTTP annotation verb.",
                    "Align HTTP verbs with REST semantics (POST create, PUT/PATCH update, DELETE remove)."),
            new Rule(
                    "API_MISSING_VALIDATION",
                    Pattern.compile("(?i)@(Post|Put|Patch)Mapping[\\s\\S]{0,200}\\([^)]*\\)\\s*\\{"),
                    ReviewSeverity.SUGGESTION,
                    "Write endpoint may lack validation annotations",
                    "Write mapping appears without nearby @Valid/@Validated in the excerpt.",
                    "Annotate request bodies with @Valid and bean validation constraints."),
            new Rule(
                    "API_MISSING_RESPONSE_TYPE",
                    Pattern.compile("(?i)@(Get|Post|Put|Delete|Patch)Mapping[\\s\\S]{0,120}public\\s+ResponseEntity\\s*<\\s*\\?"),
                    ReviewSeverity.SUGGESTION,
                    "Wildcard ResponseEntity return type",
                    "Controller method returns ResponseEntity<?> which weakens API contracts.",
                    "Use a concrete DTO response type for OpenAPI and clients."),
            new Rule(
                    "API_MISSING_PAGINATION",
                    Pattern.compile("(?i)@GetMapping[\\s\\S]{0,200}List\\s*<"),
                    ReviewSeverity.WARNING,
                    "List endpoint without pagination signals",
                    "GET endpoint returns a List without Page/Pageable/limit parameters in the excerpt.",
                    "Add pagination (Pageable/limit/cursor) for collection endpoints."),
            new Rule(
                    "API_MISSING_OPENAPI",
                    Pattern.compile("(?i)@RestController[\\s\\S]{0,400}(?!.*@(Operation|Tag|Schema))"),
                    ReviewSeverity.INFO,
                    "Controller without OpenAPI annotations",
                    "Rest controller changes do not show OpenAPI annotations nearby.",
                    "Add @Tag/@Operation/@Schema for discoverable API documentation."),
            new Rule(
                    "API_INCONSISTENT_NAMING",
                    Pattern.compile("(?i)@(Get|Post|Put|Delete|Patch)Mapping\\(\"/api/[A-Z]"),
                    ReviewSeverity.SUGGESTION,
                    "Inconsistent endpoint casing",
                    "Endpoint path starts with an uppercase segment after /api/.",
                    "Use lowercase kebab-case path segments consistently."));

    public List<FindingDraft> analyze(String content) {
        return analyze(ReviewContext.of(content));
    }

    public List<FindingDraft> analyze(ReviewContext context) {
        return PatternRuleEngine.apply(ReviewCategory.ApiDesign, context.safeContent(), RULES);
    }
}
