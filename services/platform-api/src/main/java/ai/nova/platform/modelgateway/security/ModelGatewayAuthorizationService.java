package ai.nova.platform.modelgateway.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class ModelGatewayAuthorizationService {

    public static final String MODEL_PROVIDER_READ = "MODEL_PROVIDER_READ";
    public static final String MODEL_PROVIDER_CREATE = "MODEL_PROVIDER_CREATE";
    public static final String MODEL_PROVIDER_UPDATE = "MODEL_PROVIDER_UPDATE";
    public static final String MODEL_PROVIDER_ACTIVATE = "MODEL_PROVIDER_ACTIVATE";
    public static final String MODEL_PROVIDER_DISABLE = "MODEL_PROVIDER_DISABLE";
    public static final String MODEL_PROVIDER_ARCHIVE = "MODEL_PROVIDER_ARCHIVE";
    public static final String MODEL_READ = "MODEL_READ";
    public static final String MODEL_CREATE = "MODEL_CREATE";
    public static final String MODEL_UPDATE = "MODEL_UPDATE";
    public static final String MODEL_ACTIVATE = "MODEL_ACTIVATE";
    public static final String MODEL_DISABLE = "MODEL_DISABLE";
    public static final String MODEL_ARCHIVE = "MODEL_ARCHIVE";
    public static final String MODEL_PROJECT_ASSIGN = "MODEL_PROJECT_ASSIGN";
    public static final String MODEL_AGENT_ASSIGN = "MODEL_AGENT_ASSIGN";
    public static final String MODEL_ROUTE_READ = "MODEL_ROUTE_READ";
    public static final String MODEL_ROUTE_MANAGE = "MODEL_ROUTE_MANAGE";
    public static final String MODEL_USAGE_READ = "MODEL_USAGE_READ";
    public static final String MODEL_INVOKE = "MODEL_INVOKE";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
