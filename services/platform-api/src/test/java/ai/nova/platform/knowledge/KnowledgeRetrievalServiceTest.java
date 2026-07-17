package ai.nova.platform.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import ai.nova.platform.agent.runtime.RuntimeKnowledgeContext;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeAssignRequest;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.CreateKnowledgeBaseRequest;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeBaseResponse;
import ai.nova.platform.knowledge.service.AgentKnowledgeAssignmentService;
import ai.nova.platform.knowledge.service.KnowledgeBaseService;
import ai.nova.platform.knowledge.service.KnowledgeDocumentService;
import ai.nova.platform.knowledge.service.KnowledgeRetrievalService;
import ai.nova.platform.knowledge.service.KnowledgeRetrievalService.RetrievalResult;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest
class KnowledgeRetrievalServiceTest {

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
    void retrievesChunksWithCitationsAndDedupes() {
        AuthenticatedUser user = new AuthenticatedUser(
                USER_ID, ORG_ID, "admin@nova.local", "Admin", List.of("ORG_ADMIN"), List.of("KNOWLEDGE_RETRIEVE"), true);
        String key = "RET_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        KnowledgeBaseResponse kb = knowledgeBaseService.create(
                PROJECT_ID,
                new CreateKnowledgeBaseRequest(key, "Retrieval KB", null, "DETERMINISTIC_LOCAL", 300, 30, 5, null),
                user);
        knowledgeBaseService.activate(PROJECT_ID, kb.id(), user);
        assignmentService.assign(PROJECT_ID, AGENT_ID, new AgentKnowledgeAssignRequest(kb.id(), 5, null), user);

        String content = "Refund policy allows returns within thirty days for unused products.";
        documentService.upload(
                PROJECT_ID,
                kb.id(),
                new MockMultipartFile("file", "policy.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8)),
                "POLICY",
                user);

        RetrievalResult result = retrievalService.retrieve(
                PROJECT_ID, AGENT_ID, "refund policy returns", null, null, user);
        RuntimeKnowledgeContext context = result.context();
        assertThat(context.chunks()).isNotEmpty();
        assertThat(context.citations()).hasSameSizeAs(context.chunks());
        assertThat(context.citations().getFirst().label()).startsWith("K");
        assertThat(context.citations().stream().map(c -> c.knowledgeBaseId())).contains(kb.id());
        assertThat(context.chunks().stream().anyMatch(c -> c.content().toLowerCase().contains("refund")))
                .isTrue();
    }
}
