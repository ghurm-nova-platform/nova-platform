package ai.nova.platform.prreview.service;

import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewSeverity;
import ai.nova.platform.prreview.service.PatternRuleEngine.Rule;

@Service
public class InfrastructureReviewService {

    private static final List<Rule> RULES = List.of(
            new Rule(
                    "INFRA_PRIVILEGED_CONTAINER",
                    Pattern.compile("(?i)privileged:\\s*true"),
                    ReviewSeverity.ERROR,
                    "Privileged container",
                    "Diff enables privileged container mode.",
                    "Avoid privileged mode; grant only required capabilities."),
            new Rule(
                    "INFRA_LATEST_TAG",
                    Pattern.compile("(?i)image:\\s*[^\\s]+?:latest\\b|FROM\\s+[^\\s]+?:latest\\b"),
                    ReviewSeverity.WARNING,
                    "Floating :latest image tag",
                    "Diff references an image with the mutable :latest tag.",
                    "Pin images to immutable digests or explicit versions."),
            new Rule(
                    "INFRA_HOST_NETWORK",
                    Pattern.compile("(?i)network_mode:\\s*host|hostNetwork:\\s*true"),
                    ReviewSeverity.WARNING,
                    "Host networking enabled",
                    "Diff enables host networking which weakens isolation.",
                    "Prefer bridge/CNI networking unless host network is required."),
            new Rule(
                    "INFRA_INSECURE_HTTP",
                    Pattern.compile("(?i)(image|url|endpoint|healthcheck)\\s*[:=]\\s*[\"']?http://"),
                    ReviewSeverity.SUGGESTION,
                    "Plain HTTP endpoint in infra config",
                    "Infrastructure/config diff configures an http:// URL for an image/endpoint/healthcheck.",
                    "Prefer HTTPS for external endpoints and health checks."),
            new Rule(
                    "INFRA_WILDCARD_CORS",
                    Pattern.compile("(?i)Access-Control-Allow-Origin\\s*[:=]\\s*\\*|allowedOrigins?\\s*[:=]\\s*\\*"),
                    ReviewSeverity.WARNING,
                    "Wildcard CORS origin",
                    "Diff configures CORS to allow any origin.",
                    "Restrict allowed origins to known frontend hosts."));

    public List<FindingDraft> analyze(String content) {
        return analyze(ReviewContext.of(content));
    }

    public List<FindingDraft> analyze(ReviewContext context) {
        return PatternRuleEngine.apply(ReviewCategory.Infrastructure, context.safeContent(), RULES);
    }
}
