package ai.nova.platform.conversation.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class ConversationAuthorizationService {

    public static final String CONVERSATION_READ = "CONVERSATION_READ";
    public static final String CONVERSATION_CREATE = "CONVERSATION_CREATE";
    public static final String CONVERSATION_UPDATE = "CONVERSATION_UPDATE";
    public static final String CONVERSATION_ARCHIVE = "CONVERSATION_ARCHIVE";
    public static final String CONVERSATION_MESSAGE_READ = "CONVERSATION_MESSAGE_READ";
    public static final String CONVERSATION_MESSAGE_CREATE = "CONVERSATION_MESSAGE_CREATE";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
