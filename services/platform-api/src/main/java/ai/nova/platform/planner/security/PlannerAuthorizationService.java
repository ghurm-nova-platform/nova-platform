package ai.nova.platform.planner.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class PlannerAuthorizationService {

    public static final String PLANNER_PLAN = "PLANNER_PLAN";
    public static final String PLANNER_IMPORT = "PLANNER_IMPORT";
    public static final String PLANNER_TEMPLATE_READ = "PLANNER_TEMPLATE_READ";
    public static final String PLANNER_TEMPLATE_MANAGE = "PLANNER_TEMPLATE_MANAGE";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
