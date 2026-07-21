package ai.nova.platform.identity.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.identity.error.IdentityErrorCodes;
import ai.nova.platform.identity.permission.IdentityPermissionCodes;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class IdentityAuthorizationService {

    public void requireRead(AuthenticatedUser user) {
        require(user, IdentityPermissionCodes.IDENTITY_READ);
    }

    public void requireAdmin(AuthenticatedUser user) {
        require(user, IdentityPermissionCodes.IDENTITY_ADMIN);
    }

    public void requireProviderAdmin(AuthenticatedUser user) {
        requireAny(user, IdentityPermissionCodes.IDENTITY_PROVIDER_ADMIN, IdentityPermissionCodes.IDENTITY_ADMIN);
    }

    public void requireSessionAdmin(AuthenticatedUser user) {
        requireAny(user, IdentityPermissionCodes.IDENTITY_SESSION_ADMIN, IdentityPermissionCodes.IDENTITY_ADMIN);
    }

    public void requireUserAdmin(AuthenticatedUser user) {
        requireAny(user, IdentityPermissionCodes.IDENTITY_USER_ADMIN, IdentityPermissionCodes.IDENTITY_ADMIN);
    }

    public void requireGroupAdmin(AuthenticatedUser user) {
        requireAny(user, IdentityPermissionCodes.IDENTITY_GROUP_ADMIN, IdentityPermissionCodes.IDENTITY_ADMIN);
    }

    public void requireRoleAdmin(AuthenticatedUser user) {
        requireAny(user, IdentityPermissionCodes.IDENTITY_ROLE_ADMIN, IdentityPermissionCodes.IDENTITY_ADMIN);
    }

    public void requirePermissionAdmin(AuthenticatedUser user) {
        requireAny(user, IdentityPermissionCodes.IDENTITY_PERMISSION_ADMIN, IdentityPermissionCodes.IDENTITY_ADMIN);
    }

    public void requireAuditRead(AuthenticatedUser user) {
        requireAny(user, IdentityPermissionCodes.IDENTITY_AUDIT_READ, IdentityPermissionCodes.IDENTITY_READ);
    }

    public void requireMfaManage(AuthenticatedUser user) {
        requireAny(user, IdentityPermissionCodes.IDENTITY_MFA_MANAGE, IdentityPermissionCodes.IDENTITY_ADMIN);
    }

    public void requireScimProvision(AuthenticatedUser user) {
        require(user, IdentityPermissionCodes.SCIM_PROVISION);
    }

    /** @deprecated use {@link #requireProviderAdmin} */
    @Deprecated
    public void requireProviderManage(AuthenticatedUser user) {
        requireProviderAdmin(user);
    }

    public void require(AuthenticatedUser user, String permission) {
        if (user != null && user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (user == null || !user.hasPermission(permission)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, IdentityErrorCodes.PERMISSION_DENIED, "Missing permission: " + permission);
        }
    }

    private void requireAny(AuthenticatedUser user, String... permissions) {
        if (user != null && user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (user != null) {
            for (String permission : permissions) {
                if (user.hasPermission(permission)) {
                    return;
                }
            }
        }
        throw new ApiException(
                HttpStatus.FORBIDDEN,
                IdentityErrorCodes.PERMISSION_DENIED,
                "Missing one of permissions: " + String.join(", ", permissions));
    }
}
