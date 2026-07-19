package ai.nova.platform.release.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

public final class ReleaseTestFixture {

    public static final UUID ORG_ID = PullRequestTestFixture.ORG_ID;
    public static final UUID USER_ID = PullRequestTestFixture.USER_ID;
    public static final UUID PROJECT_ID = PullRequestTestFixture.PROJECT_UUID;

    private ReleaseTestFixture() {
    }

    public static AuthenticatedUser releaseAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("RELEASE_RUN", "RELEASE_READ"),
                true);
    }

    public static AuthenticatedUser releaseReadOnlyUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "viewer@nova.local",
                "Viewer",
                List.of("USER"),
                List.of("RELEASE_READ"),
                true);
    }

    public static String createBody(String name, String version, UUID mergeId, String commitSha) {
        return """
                {
                  "projectId":"%s",
                  "releaseName":"%s",
                  "description":"Sprint 4 release",
                  "bumpType":"PATCH",
                  "semanticVersion":%s,
                  "mergeOperationIds":["%s"],
                  "approvalDecisionIds":["%s"],
                  "pullRequestIds":["%s"],
                  "patchIds":["%s"],
                  "commitShas":["%s"],
                  "artifacts":[{"artifactType":"MANIFEST_REF","artifactUri":"memory://artifact/%s","artifactHash":"abc","label":"ref"}]
                }
                """.formatted(
                PROJECT_ID,
                name,
                version == null ? "null" : "\"" + version + "\"",
                mergeId,
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1001"),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1002"),
                UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa1003"),
                commitSha,
                commitSha);
    }
}
