package ai.nova.platform.knowledge.engine.support;

import java.util.List;
import java.util.UUID;

import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.CreateDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.ImportDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.RelateDocumentRequest;
import ai.nova.platform.knowledge.engine.entity.Category;
import ai.nova.platform.knowledge.engine.entity.ContentFormat;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.entity.RelationType;
import ai.nova.platform.knowledge.engine.entity.Visibility;
import ai.nova.platform.knowledge.engine.security.KnowledgeEngineAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

public final class KnowledgeEngineTestFixture {

    public static final UUID ORG_ID = AuditTestFixture.ORG_ID;
    public static final UUID USER_ID = AuditTestFixture.USER_ID;
    public static final UUID PROJECT_ID = AuditTestFixture.PROJECT_ID;
    public static final UUID OTHER_USER_ID = UUID.fromString("99999999-9999-9999-9999-999999999998");

    private KnowledgeEngineTestFixture() {
    }

    public static AuthenticatedUser knowledgeReadUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "knowledge-reader@nova.local",
                "Knowledge Reader",
                List.of("USER"),
                List.of(KnowledgeEngineAuthorizationService.KNOWLEDGE_READ),
                true);
    }

    public static AuthenticatedUser knowledgeWriteUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "knowledge-writer@nova.local",
                "Knowledge Writer",
                List.of("USER"),
                List.of(
                        KnowledgeEngineAuthorizationService.KNOWLEDGE_READ,
                        KnowledgeEngineAuthorizationService.KNOWLEDGE_WRITE),
                true);
    }

    public static AuthenticatedUser knowledgeAdminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "knowledge-admin@nova.local",
                "Knowledge Admin",
                List.of("USER"),
                List.of(
                        KnowledgeEngineAuthorizationService.KNOWLEDGE_READ,
                        KnowledgeEngineAuthorizationService.KNOWLEDGE_WRITE,
                        KnowledgeEngineAuthorizationService.KNOWLEDGE_ADMIN),
                true);
    }

    public static AuthenticatedUser knowledgeNoPermissionUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "knowledge-noperm@nova.local",
                "No Knowledge Perm",
                List.of("USER"),
                List.of("ENVIRONMENT_READ"),
                true);
    }

    public static AuthenticatedUser privateDocumentReader() {
        return new AuthenticatedUser(
                OTHER_USER_ID,
                ORG_ID,
                "knowledge-other@nova.local",
                "Other Reader",
                List.of("USER"),
                List.of(KnowledgeEngineAuthorizationService.KNOWLEDGE_READ),
                true);
    }

    public static String uniqueTitle(String prefix) {
        return prefix + "-" + UUID.randomUUID();
    }

    public static CreateDocumentRequest sampleCreateRequest(String title) {
        return new CreateDocumentRequest(
                PROJECT_ID,
                title,
                "Sample summary for " + title,
                "# " + title + "\n\nSample knowledge content with searchable keyword alpha.",
                ContentFormat.MARKDOWN,
                KnowledgeType.DOCUMENTATION,
                Category.General,
                Visibility.ORGANIZATION,
                List.of("platform", "docs"));
    }

    public static CreateDocumentRequest privateCreateRequest(String title) {
        return new CreateDocumentRequest(
                PROJECT_ID,
                title,
                "Private summary",
                "Private content body",
                ContentFormat.PLAIN_TEXT,
                KnowledgeType.DOCUMENTATION,
                Category.General,
                Visibility.PRIVATE,
                List.of("private"));
    }

    public static ImportDocumentRequest adrImportRequest(String title) {
        return new ImportDocumentRequest(
                PROJECT_ID,
                title,
                null,
                "# ADR-0001 Use PostgreSQL\n\n## Status\nAccepted\n\n## Context\nWe need durable storage.",
                null,
                null,
                Category.Architecture,
                Visibility.ORGANIZATION,
                List.of("adr"),
                "adr");
    }

    public static RelateDocumentRequest relateRequest(UUID targetDocumentId) {
        return new RelateDocumentRequest(RelationType.REFERENCES, targetDocumentId, null, null);
    }

    public static String createDocumentBody(String title) {
        return """
                {
                  "projectId":"%s",
                  "title":"%s",
                  "summary":"Controller summary",
                  "content":"# %s\\n\\nController content",
                  "contentFormat":"MARKDOWN",
                  "knowledgeType":"DOCUMENTATION",
                  "category":"General",
                  "visibility":"ORGANIZATION",
                  "tags":["controller"]
                }
                """.formatted(PROJECT_ID, title, title);
    }
}
