package ai.nova.platform.llm.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.environment.support.EnvironmentTestFixture;
import ai.nova.platform.llm.permission.LlmPermissionCodes;
import ai.nova.platform.security.AuthenticatedUser;

public final class LlmTestFixture {

    public static final UUID ORG_ID = EnvironmentTestFixture.ORG_ID;
    public static final UUID USER_ID = EnvironmentTestFixture.USER_ID;
    public static final UUID SEED_MODEL_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaa01");

    private LlmTestFixture() {
    }

    public static AuthenticatedUser llmAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        LlmPermissionCodes.LLM_READ,
                        LlmPermissionCodes.LLM_ADMIN,
                        LlmPermissionCodes.LLM_INFER,
                        LlmPermissionCodes.LLM_MODEL_ADMIN,
                        LlmPermissionCodes.LLM_PROMPT_ADMIN),
                true);
    }

    public static AuthenticatedUser llmReadUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "llm-reader@nova.local",
                "LLM Reader",
                List.of("USER"),
                List.of(LlmPermissionCodes.LLM_READ),
                true);
    }

    public static AuthenticatedUser llmInferUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "llm-infer@nova.local",
                "LLM Infer",
                List.of("USER"),
                List.of(LlmPermissionCodes.LLM_READ, LlmPermissionCodes.LLM_INFER),
                true);
    }
}
