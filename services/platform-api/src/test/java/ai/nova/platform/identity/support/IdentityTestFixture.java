package ai.nova.platform.identity.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.environment.support.EnvironmentTestFixture;
import ai.nova.platform.identity.permission.IdentityPermissionCodes;
import ai.nova.platform.security.AuthenticatedUser;

public final class IdentityTestFixture {

    public static final UUID ORG_ID = EnvironmentTestFixture.ORG_ID;
    public static final UUID USER_ID = EnvironmentTestFixture.USER_ID;
    public static final UUID IDENTITY_USER_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");

    private IdentityTestFixture() {
    }

    public static AuthenticatedUser identityAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        IdentityPermissionCodes.IDENTITY_READ,
                        IdentityPermissionCodes.IDENTITY_ADMIN,
                        IdentityPermissionCodes.IDENTITY_PROVIDER_ADMIN,
                        IdentityPermissionCodes.IDENTITY_SESSION_ADMIN,
                        IdentityPermissionCodes.IDENTITY_USER_ADMIN,
                        IdentityPermissionCodes.IDENTITY_GROUP_ADMIN,
                        IdentityPermissionCodes.IDENTITY_ROLE_ADMIN,
                        IdentityPermissionCodes.IDENTITY_PERMISSION_ADMIN,
                        IdentityPermissionCodes.IDENTITY_AUDIT_READ,
                        IdentityPermissionCodes.IDENTITY_MFA_MANAGE,
                        IdentityPermissionCodes.SCIM_PROVISION),
                true);
    }

    public static AuthenticatedUser identityReadUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "identity-reader@nova.local",
                "Identity Reader",
                List.of("USER"),
                List.of(IdentityPermissionCodes.IDENTITY_READ),
                true);
    }

    public static AuthenticatedUser scimProvisioner() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "scim@nova.local",
                "SCIM Provisioner",
                List.of("USER"),
                List.of(IdentityPermissionCodes.SCIM_PROVISION),
                true);
    }
}
