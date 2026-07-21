package ai.nova.platform.knowledge.engine.service;

import org.springframework.stereotype.Service;

import ai.nova.platform.knowledge.engine.entity.KnowledgeDocumentEntity;
import ai.nova.platform.knowledge.engine.entity.Visibility;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class KnowledgeVisibilityService {

    public boolean canRead(AuthenticatedUser user, KnowledgeDocumentEntity document) {
        if (user == null || document == null) {
            return false;
        }
        if (!user.getOrganizationId().equals(document.getOrganizationId())) {
            return false;
        }
        if (user.getRoles().contains("ORG_ADMIN")) {
            return true;
        }
        return switch (document.getVisibility()) {
            case PRIVATE -> user.getUserId().equals(document.getAuthorId());
            case PROJECT -> document.getProjectId() != null;
            case ORGANIZATION, PUBLIC -> true;
        };
    }
}
