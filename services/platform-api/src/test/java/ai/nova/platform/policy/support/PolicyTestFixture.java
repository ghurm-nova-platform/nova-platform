package ai.nova.platform.policy.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.rollback.support.RollbackTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

public final class PolicyTestFixture {

    public static final UUID ORG_ID = RollbackTestFixture.ORG_ID;
    public static final UUID USER_ID = RollbackTestFixture.USER_ID;
    public static final UUID PROJECT_ID = RollbackTestFixture.PROJECT_ID;

    private PolicyTestFixture() {
    }

    public static AuthenticatedUser policyAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        "POLICY_RUN",
                        "POLICY_READ",
                        "RELEASE_RUN",
                        "RELEASE_READ",
                        "ROLLBACK_RUN",
                        "ROLLBACK_READ",
                        "DEPLOYMENT_RUN",
                        "DEPLOYMENT_READ"),
                true);
    }

    public static String createBody(String name, String type, String mode, int priority, String configJson) {
        return """
                {
                  "projectId":"%s",
                  "policyName":"%s",
                  "description":"policy test",
                  "policyType":"%s",
                  "priority":%d,
                  "evaluationMode":"%s",
                  "configuration":%s
                }
                """.formatted(PROJECT_ID, name, type, priority, mode, configJson);
    }

    public static String evaluateBody(UUID releaseId) {
        return "{\"releaseId\":\"%s\"}".formatted(releaseId);
    }

    public static long uniquePatch() {
        return Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
    }
}
