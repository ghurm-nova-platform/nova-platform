package ai.nova.platform.deploymentexecution.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.rollback.support.RollbackTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

public final class ExecutionTestFixture {

    public static final UUID ORG_ID = RollbackTestFixture.ORG_ID;
    public static final UUID USER_ID = RollbackTestFixture.USER_ID;
    public static final UUID PROJECT_ID = RollbackTestFixture.PROJECT_ID;
    public static final UUID STAGING_ENVIRONMENT_ID = UUID.fromString("bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbb004");

    private ExecutionTestFixture() {
    }

    public static AuthenticatedUser executionAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        "EXECUTION_RUN",
                        "EXECUTION_READ",
                        "ROLLBACK_RUN",
                        "ROLLBACK_READ",
                        "DEPLOYMENT_RUN",
                        "DEPLOYMENT_READ",
                        "RELEASE_RUN",
                        "RELEASE_READ"),
                true);
    }

    public static String createBody(UUID releaseId, UUID environmentId, String provider) {
        return """
                {
                  "releaseId":"%s",
                  "environmentId":"%s",
                  "provider":"%s"
                }
                """.formatted(releaseId, environmentId, provider);
    }

    public static String uniqueVersion(String prefix) {
        return RollbackTestFixture.uniqueVersion("exec-" + prefix);
    }
}
