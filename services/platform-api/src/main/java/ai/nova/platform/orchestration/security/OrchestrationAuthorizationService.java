package ai.nova.platform.orchestration.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class OrchestrationAuthorizationService {

    public static final String ORCHESTRATION_RUN_READ = "ORCHESTRATION_RUN_READ";
    public static final String ORCHESTRATION_RUN_CREATE = "ORCHESTRATION_RUN_CREATE";
    public static final String ORCHESTRATION_RUN_UPDATE = "ORCHESTRATION_RUN_UPDATE";
    public static final String ORCHESTRATION_RUN_START = "ORCHESTRATION_RUN_START";
    public static final String ORCHESTRATION_RUN_CANCEL = "ORCHESTRATION_RUN_CANCEL";
    public static final String ORCHESTRATION_RUN_ARCHIVE = "ORCHESTRATION_RUN_ARCHIVE";
    public static final String ORCHESTRATION_TASK_MANAGE = "ORCHESTRATION_TASK_MANAGE";
    public static final String ORCHESTRATION_TASK_EXECUTE = "ORCHESTRATION_TASK_EXECUTE";
    public static final String ORCHESTRATION_EVENT_READ = "ORCHESTRATION_EVENT_READ";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
