package ai.nova.platform.execution.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class ExecutionAuthorizationService {

    public static final String AGENT_EXECUTE = "AGENT_EXECUTE";
    public static final String EXECUTION_READ = "EXECUTION_READ";
    public static final String EXECUTION_CANCEL = "EXECUTION_CANCEL";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
