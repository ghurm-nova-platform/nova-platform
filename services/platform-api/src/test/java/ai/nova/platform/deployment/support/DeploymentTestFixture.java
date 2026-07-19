package ai.nova.platform.deployment.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

public final class DeploymentTestFixture {

    public static final UUID ORG_ID = PullRequestTestFixture.ORG_ID;
    public static final UUID USER_ID = PullRequestTestFixture.USER_ID;
    public static final UUID PROJECT_ID = PullRequestTestFixture.PROJECT_UUID;

    private DeploymentTestFixture() {
    }

    public static AuthenticatedUser deploymentAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("DEPLOYMENT_RUN", "DEPLOYMENT_READ", "RELEASE_RUN", "RELEASE_READ"),
                true);
    }

    public static String uniqueVersion(String prefix) {
        // Avoid colliding with ReleaseManagerServiceTest fixed versions (1.x–9.x) in shared H2.
        long mid = Math.floorMod((long) prefix.hashCode(), 900L) + 100L;
        long patch = Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
        return "80." + mid + "." + patch;
    }

    public static String observeBody(
            UUID releaseId, String environment, String provider, String externalKey, String status, String health) {
        return """
                {
                  "releaseId":"%s",
                  "environment":"%s",
                  "status":"%s",
                  "health":"%s",
                  "healthMessage":"ok",
                  "deploymentProvider":"%s",
                  "externalDeploymentKey":"%s",
                  "startedAt":"2026-07-19T18:00:00Z",
                  "logMetadata":"build=42",
                  "artifacts":[{"artifactType":"IMAGE","artifactUri":"memory://img/%s","artifactHash":"abc","label":"app"}]
                }
                """.formatted(releaseId, environment, status, health, provider, externalKey, externalKey);
    }
}
