package ai.nova.platform.identity.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

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

    public void requireProviderManage(AuthenticatedUser user) {
        require(user, IdentityPermissionCodes.IDENTITY_PROVIDER_MANAGE);
    }

    public void requireMfaManage(AuthenticatedUser user) {
        require(user, IdentityPermissionCodes.IDENTITY_MFA_MANAGE);
    }

    public void requireScimProvision(AuthenticatedUser user) {
        require(user, IdentityPermissionCodes.SCIM_PROVISION);
    }

    public void require(AuthenticatedUser user, String permission) {
        if (user != null && user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (user == null || !user.hasPermission(permission)) {
            throw new ApiException(
                    HttpStatus.FORBIDDEN, "IDENTITY_PERMISSION_DENIED", "Missing permission: " + permission);
        }
    }
}
