package ai.nova.platform.knowledge.vector;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeDocumentResponse;
import ai.nova.platform.knowledge.embedding.DeterministicLocalEmbeddingProvider;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.repository.KnowledgeEmbeddingRepository;
import ai.nova.platform.knowledge.service.KnowledgeDocumentService;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest
class DatabaseKnowledgeVectorStoreTest {

    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID DEMO_KB_ID = UUID.fromString("88888888-8888-8888-8888-888888888801");

    @Autowired
    private DatabaseKnowledgeVectorStore vectorStore;
    @Autowired
    private KnowledgeDocumentService documentService;
    @Autowired
    private DeterministicLocalEmbeddingProvider embeddingProvider;
    @Autowired
    private KnowledgeEmbeddingRepository embeddingRepository;

    @Test
    void searchesReadyDocumentsWithStableOrdering() {
        AuthenticatedUser user = new AuthenticatedUser(
                USER_ID, ORG_ID, "admin@nova.local", "Admin", List.of("ORG_ADMIN"), List.of(), true);

        String unique = "VECTOR_STORE_TOKEN_" + UUID.randomUUID();
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "guide.txt",
                "text/plain",
                ("Nova platform knowledge retrieval uses deterministic local embeddings. " + unique)
                        .getBytes(StandardCharsets.UTF_8));
        KnowledgeDocumentResponse doc = documentService.upload(
                PROJECT_ID,
                DEMO_KB_ID,
                file,
                "VEC_" + UUID.randomUUID().toString().substring(0, 8).toUpperCase(),
                user);
        assertThat(doc.status()).isEqualTo(KnowledgeDocumentStatus.READY);
        assertThat(embeddingRepository.findCandidates(
                        ORG_ID, PROJECT_ID, DEMO_KB_ID, "DETERMINISTIC_LOCAL", "deterministic-v1"))
                .isNotEmpty();

        float[] query = embeddingProvider.embed(unique);
        List<VectorSearchHit> hits = vectorStore.search(
                ORG_ID,
                PROJECT_ID,
                DEMO_KB_ID,
                "DETERMINISTIC_LOCAL",
                "deterministic-v1",
                query,
                5,
                -1.0,
                1000);
        assertThat(hits).isNotEmpty();
        for (int i = 1; i < hits.size(); i++) {
            assertThat(hits.get(i - 1).score()).isGreaterThanOrEqualTo(hits.get(i).score());
            if (Double.compare(hits.get(i - 1).score(), hits.get(i).score()) == 0) {
                assertThat(hits.get(i - 1).chunkId().compareTo(hits.get(i).chunkId())).isLessThanOrEqualTo(0);
            }
        }
    }
}
