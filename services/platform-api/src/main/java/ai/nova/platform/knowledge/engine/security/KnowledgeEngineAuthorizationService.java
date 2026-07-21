package ai.nova.platform.knowledge.engine.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class KnowledgeEngineAuthorizationService {

    public static final String KNOWLEDGE_READ = "KNOWLEDGE_READ";
    public static final String KNOWLEDGE_WRITE = "KNOWLEDGE_WRITE";
    public static final String KNOWLEDGE_ADMIN = "KNOWLEDGE_ADMIN";

    public void requireRead(AuthenticatedUser user) {
        require(user, KNOWLEDGE_READ);
    }

    public void requireWrite(AuthenticatedUser user) {
        require(user, KNOWLEDGE_WRITE);
    }

    public void requireAdmin(AuthenticatedUser user) {
        require(user, KNOWLEDGE_ADMIN);
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
