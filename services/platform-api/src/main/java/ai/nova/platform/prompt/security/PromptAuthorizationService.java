package ai.nova.platform.prompt.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class PromptAuthorizationService {

    public static final String PROMPT_READ = "PROMPT_READ";
    public static final String PROMPT_CREATE = "PROMPT_CREATE";
    public static final String PROMPT_UPDATE = "PROMPT_UPDATE";
    public static final String PROMPT_PUBLISH = "PROMPT_PUBLISH";
    public static final String PROMPT_ARCHIVE = "PROMPT_ARCHIVE";
    public static final String PROMPT_COMPARE = "PROMPT_COMPARE";
    public static final String PROMPT_PREVIEW = "PROMPT_PREVIEW";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
