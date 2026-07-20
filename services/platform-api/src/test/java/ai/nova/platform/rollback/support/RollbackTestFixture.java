package ai.nova.platform.rollback.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.deployment.support.DeploymentTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

public final class RollbackTestFixture {

    public static final UUID ORG_ID = DeploymentTestFixture.ORG_ID;
    public static final UUID USER_ID = DeploymentTestFixture.USER_ID;
    public static final UUID PROJECT_ID = DeploymentTestFixture.PROJECT_ID;

    private RollbackTestFixture() {
    }

    public static AuthenticatedUser rollbackAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        "ROLLBACK_RUN",
                        "ROLLBACK_READ",
                        "DEPLOYMENT_RUN",
                        "DEPLOYMENT_READ",
                        "RELEASE_RUN",
                        "RELEASE_READ"),
                true);
    }

    public static String uniqueVersion(String prefix) {
        return DeploymentTestFixture.uniqueVersion("rb-" + prefix);
    }

    public static String createBody(
            UUID releaseId,
            UUID deploymentId,
            UUID targetReleaseId,
            String environment,
            String strategy,
            String reason) {
        return """
                {
                  "releaseId":"%s",
                  "deploymentId":"%s",
                  "targetReleaseId":"%s",
                  "environment":"%s",
                  "strategy":"%s",
                  "reason":"%s",
                  "riskLevel":"MEDIUM"
                }
                """.formatted(releaseId, deploymentId, targetReleaseId, environment, strategy, reason);
    }
}
