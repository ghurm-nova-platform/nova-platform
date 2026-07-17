package ai.nova.platform.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeAssignRequest;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.CreateKnowledgeBaseRequest;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeBaseResponse;
import ai.nova.platform.knowledge.service.AgentKnowledgeAssignmentService;
import ai.nova.platform.knowledge.service.KnowledgeBaseService;
import ai.nova.platform.knowledge.service.KnowledgeDocumentService;
import ai.nova.platform.knowledge.service.KnowledgeRetrievalService;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest
class KnowledgeRetrievalIsolationTest {

    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID AGENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");

    @Autowired
    private KnowledgeRetrievalService retrievalService;
    @Autowired
    private KnowledgeBaseService knowledgeBaseService;
    @Autowired
    private KnowledgeDocumentService documentService;
    @Autowired
    private AgentKnowledgeAssignmentService assignmentService;

    @Test
    void archivedKnowledgeBaseIsExcludedFromRetrieval() {
        AuthenticatedUser user = new AuthenticatedUser(
                USER_ID, ORG_ID, "admin@nova.local", "Admin", List.of("ORG_ADMIN"), List.of("KNOWLEDGE_RETRIEVE"), true);
        String key = "ISO_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        KnowledgeBaseResponse kb = knowledgeBaseService.create(
                PROJECT_ID,
                new CreateKnowledgeBaseRequest(key, "Isolated KB", null, "DETERMINISTIC_LOCAL", 300, 30, 5, null),
                user);
        knowledgeBaseService.activate(PROJECT_ID, kb.id(), user);
        assignmentService.assign(PROJECT_ID, AGENT_ID, new AgentKnowledgeAssignRequest(kb.id(), null, null), user);
        documentService.upload(
                PROJECT_ID,
                kb.id(),
                new MockMultipartFile(
                        "file",
                        "secret.txt",
                        "text/plain",
                        "TOPSECRET_ISOLATION_TOKEN_XYZ".getBytes(StandardCharsets.UTF_8)),
                "SECRET",
                user);

        knowledgeBaseService.archive(PROJECT_ID, kb.id(), user);

        var result = retrievalService.retrieve(
                PROJECT_ID, AGENT_ID, "TOPSECRET_ISOLATION_TOKEN_XYZ", null, null, user);
        assertThat(result.context().chunks())
                .noneMatch(chunk -> chunk.content().contains("TOPSECRET_ISOLATION_TOKEN_XYZ")
                        && chunk.knowledgeBaseId().equals(kb.id()));
    }
}
