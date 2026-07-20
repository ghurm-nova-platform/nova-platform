package ai.nova.platform.environment.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.policy.support.PolicyTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

public final class EnvironmentTestFixture {

    public static final UUID ORG_ID = PolicyTestFixture.ORG_ID;
    public static final UUID USER_ID = PolicyTestFixture.USER_ID;
    public static final UUID PROJECT_ID = PolicyTestFixture.PROJECT_ID;

    private EnvironmentTestFixture() {
    }

    public static AuthenticatedUser environmentAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        "ENVIRONMENT_RUN",
                        "ENVIRONMENT_READ",
                        "POLICY_RUN",
                        "POLICY_READ",
                        "DEPLOYMENT_RUN",
                        "DEPLOYMENT_READ"),
                true);
    }

    public static AuthenticatedUser environmentReadOnlyUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "reader@nova.local",
                "Nova Reader",
                List.of("USER"),
                List.of("ENVIRONMENT_READ"),
                true);
    }

    public static AuthenticatedUser environmentRunOnlyUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "runner@nova.local",
                "Nova Runner",
                List.of("USER"),
                List.of("ENVIRONMENT_RUN"),
                true);
    }

    public static String createBody(String name, String environmentType) {
        return createBody(name, environmentType, PROJECT_ID);
    }

    public static String createBody(String name, String environmentType, UUID projectId) {
        return """
                {
                  "projectId":"%s",
                  "name":"%s",
                  "description":"environment test",
                  "environmentType":"%s",
                  "region":"us-east-1",
                  "provider":"kubernetes",
                  "labels":[{"key":"team","value":"platform"}],
                  "variables":[{"name":"LOG_LEVEL","description":"Log level","required":false,"masked":false,"scope":"RUNTIME"}]
                }
                """.formatted(projectId, name, environmentType);
    }

    public static String updateBody(String description) {
        return """
                {
                  "description":"%s",
                  "region":"eu-west-1"
                }
                """.formatted(description);
    }

    public static long uniquePatch() {
        return Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
    }
}
