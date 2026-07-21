package ai.nova.platform.dashboard.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class DashboardAuthorizationService {

    public static final String DASHBOARD_READ = "DASHBOARD_READ";
    public static final String DASHBOARD_ADMIN = "DASHBOARD_ADMIN";

    public void requireRead(AuthenticatedUser user) {
        require(user, DASHBOARD_READ);
    }

    public void requireAdmin(AuthenticatedUser user) {
        require(user, DASHBOARD_ADMIN);
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
