package ai.nova.platform.audit.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class AuditAuthorizationService {

    public static final String AUDIT_READ = "AUDIT_READ";
    public static final String AUDIT_WRITE = "AUDIT_WRITE";

    public void requireRead(AuthenticatedUser user) {
        require(user, AUDIT_READ);
    }

    public void requireWrite(AuthenticatedUser user) {
        require(user, AUDIT_WRITE);
    }

    public void require(AuthenticatedUser user, String permission) {
        if (user != null && user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (user == null || !user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
