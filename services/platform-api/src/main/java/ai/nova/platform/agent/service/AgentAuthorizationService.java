package ai.nova.platform.agent.service;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class AgentAuthorizationService {

    public static final String AGENT_READ = "AGENT_READ";
    public static final String AGENT_CREATE = "AGENT_CREATE";
    public static final String AGENT_UPDATE = "AGENT_UPDATE";
    public static final String AGENT_ACTIVATE = "AGENT_ACTIVATE";
    public static final String AGENT_ARCHIVE = "AGENT_ARCHIVE";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
