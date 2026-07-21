package ai.nova.platform.identity.provider;

import java.util.UUID;

import ai.nova.platform.user.UserAccount;

public record AuthenticationResult(
        boolean success,
        UserAccount platformUser,
        UUID identityUserId,
        String failureReason) {

    public static AuthenticationResult success(UserAccount platformUser, UUID identityUserId) {
        return new AuthenticationResult(true, platformUser, identityUserId, null);
    }

    public static AuthenticationResult failure(String reason) {
        return new AuthenticationResult(false, null, null, reason);
    }
}
