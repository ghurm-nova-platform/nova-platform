package ai.nova.platform.tool.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class ToolAuthorizationService {

    public static final String TOOL_READ = "TOOL_READ";
    public static final String TOOL_CREATE = "TOOL_CREATE";
    public static final String TOOL_UPDATE = "TOOL_UPDATE";
    public static final String TOOL_ACTIVATE = "TOOL_ACTIVATE";
    public static final String TOOL_ARCHIVE = "TOOL_ARCHIVE";
    public static final String TOOL_ASSIGN = "TOOL_ASSIGN";
    public static final String TOOL_EXECUTE = "TOOL_EXECUTE";
    public static final String TOOL_CALL_READ = "TOOL_CALL_READ";
    public static final String TOOL_CALL_APPROVE = "TOOL_CALL_APPROVE";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
