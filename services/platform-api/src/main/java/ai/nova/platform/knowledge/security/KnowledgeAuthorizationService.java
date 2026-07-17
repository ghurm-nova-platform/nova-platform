package ai.nova.platform.knowledge.security;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Component
public class KnowledgeAuthorizationService {

    public static final String KNOWLEDGE_READ = "KNOWLEDGE_READ";
    public static final String KNOWLEDGE_CREATE = "KNOWLEDGE_CREATE";
    public static final String KNOWLEDGE_UPDATE = "KNOWLEDGE_UPDATE";
    public static final String KNOWLEDGE_ACTIVATE = "KNOWLEDGE_ACTIVATE";
    public static final String KNOWLEDGE_ARCHIVE = "KNOWLEDGE_ARCHIVE";
    public static final String KNOWLEDGE_DOCUMENT_UPLOAD = "KNOWLEDGE_DOCUMENT_UPLOAD";
    public static final String KNOWLEDGE_DOCUMENT_READ = "KNOWLEDGE_DOCUMENT_READ";
    public static final String KNOWLEDGE_DOCUMENT_ARCHIVE = "KNOWLEDGE_DOCUMENT_ARCHIVE";
    public static final String KNOWLEDGE_DOCUMENT_REPROCESS = "KNOWLEDGE_DOCUMENT_REPROCESS";
    public static final String KNOWLEDGE_ASSIGN = "KNOWLEDGE_ASSIGN";
    public static final String KNOWLEDGE_RETRIEVE = "KNOWLEDGE_RETRIEVE";
    public static final String KNOWLEDGE_AUDIT_READ = "KNOWLEDGE_AUDIT_READ";

    public void require(AuthenticatedUser user, String permission) {
        if (user.getRoles().contains("ORG_ADMIN")) {
            return;
        }
        if (!user.hasPermission(permission)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "FORBIDDEN", "Missing permission: " + permission);
        }
    }
}
