package ai.nova.platform.prreview.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.prreview.dto.PrReviewDtos.RunRequest;
import ai.nova.platform.prreview.security.PullRequestReviewAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

public final class PrReviewTestFixture {

    public static final UUID ORG_ID = AuditTestFixture.ORG_ID;
    public static final UUID USER_ID = AuditTestFixture.USER_ID;
    public static final UUID PROJECT_ID = AuditTestFixture.PROJECT_ID;

    private PrReviewTestFixture() {
    }

    public static AuthenticatedUser prReviewReadUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "pr-review-reader@nova.local",
                "PR Review Reader",
                List.of("USER"),
                List.of(PullRequestReviewAuthorizationService.PR_REVIEW_READ),
                true);
    }

    public static AuthenticatedUser prReviewRunUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "pr-review-runner@nova.local",
                "PR Review Runner",
                List.of("USER"),
                List.of(
                        PullRequestReviewAuthorizationService.PR_REVIEW_READ,
                        PullRequestReviewAuthorizationService.PR_REVIEW_RUN,
                        PullRequestReviewAuthorizationService.PR_REVIEW_EXPORT),
                true);
    }

    public static AuthenticatedUser prReviewAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "pr-review-admin@nova.local",
                "PR Review Admin",
                List.of("USER"),
                List.of(
                        PullRequestReviewAuthorizationService.PR_REVIEW_READ,
                        PullRequestReviewAuthorizationService.PR_REVIEW_RUN,
                        PullRequestReviewAuthorizationService.PR_REVIEW_EXPORT,
                        PullRequestReviewAuthorizationService.PR_REVIEW_ADMIN),
                true);
    }

    public static RunRequest sampleRunRequest(String diffContent) {
        return new RunRequest(
                PROJECT_ID,
                null,
                42,
                "Improve checkout flow",
                "org/repo",
                "feature/checkout",
                "main",
                "abc123",
                List.of("src/CheckoutService.java"),
                diffContent);
    }

    public static String runRequestBody(String escapedDiff) {
        return """
                {
                  "projectId":"%s",
                  "pullRequestNumber":42,
                  "pullRequestTitle":"Improve checkout flow",
                  "repositoryRef":"org/repo",
                  "sourceBranch":"feature/checkout",
                  "targetBranch":"main",
                  "commitSha":"abc123",
                  "changedFiles":["src/CheckoutService.java"],
                  "diffContent":"%s"
                }
                """.formatted(PROJECT_ID, escapedDiff);
    }

    public static String cleanDiff() {
        return """
                package ai.nova.sample.service;

                public class CheckoutService {
                    public int total(int a, int b) {
                        return a + b;
                    }
                }
                """;
    }

    public static String securityDiff() {
        return """
                public class Config {
                    String password = "super-secret";
                    String api_key = "abcd";
                }
                """;
    }
}
