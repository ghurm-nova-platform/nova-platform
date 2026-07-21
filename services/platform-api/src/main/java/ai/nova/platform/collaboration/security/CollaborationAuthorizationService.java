package ai.nova.platform.collaboration.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class CollaborationAuthorizationService {

    public static final String COLLABORATION_READ = "COLLABORATION_READ";
    public static final String COLLABORATION_WRITE = "COLLABORATION_WRITE";
    public static final String COLLABORATION_ADMIN = "COLLABORATION_ADMIN";

    public void requireRead(AuthenticatedUser user) {
        require(user, COLLABORATION_READ);
    }

    public void requireWrite(AuthenticatedUser user) {
        require(user, COLLABORATION_WRITE);
    }

    public void requireAdmin(AuthenticatedUser user) {
        require(user, COLLABORATION_ADMIN);
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
