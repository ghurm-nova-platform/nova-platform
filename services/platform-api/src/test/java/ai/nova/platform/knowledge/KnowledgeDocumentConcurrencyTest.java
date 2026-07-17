package ai.nova.platform.knowledge;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;

import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentRepository;
import ai.nova.platform.knowledge.service.KnowledgeDocumentService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
class KnowledgeDocumentConcurrencyTest {

    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID DEMO_KB_ID = UUID.fromString("88888888-8888-8888-8888-888888888801");

    @Autowired
    private KnowledgeDocumentService documentService;
    @Autowired
    private KnowledgeDocumentRepository documentRepository;

    @Test
    void concurrentDuplicateUploadsDoNotCreateMultipleReadyDocs() throws Exception {
        AuthenticatedUser user = new AuthenticatedUser(
                USER_ID, ORG_ID, "admin@nova.local", "Admin", List.of("ORG_ADMIN"), List.of(), true);
        byte[] bytes = ("Concurrent duplicate " + UUID.randomUUID()).getBytes(StandardCharsets.UTF_8);
        String contentHash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes));

        ExecutorService executor = Executors.newFixedThreadPool(4);
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            final int idx = i;
            tasks.add(() -> {
                try {
                    return documentService.upload(
                            PROJECT_ID,
                            DEMO_KB_ID,
                            new MockMultipartFile("file", "c" + idx + ".txt", "text/plain", bytes),
                            "CONC_" + idx + "_" + UUID.randomUUID().toString().substring(0, 4).toUpperCase(),
                            user);
                } catch (ApiException ex) {
                    return ex.getCode();
                }
            });
        }
        List<Future<Object>> futures = executor.invokeAll(tasks);
        executor.shutdown();

        int successes = 0;
        int duplicates = 0;
        for (Future<Object> future : futures) {
            Object value = future.get();
            if (value instanceof String code && "DOCUMENT_DUPLICATE_CONTENT".equals(code)) {
                duplicates++;
            } else {
                successes++;
            }
        }
        assertThat(successes).isEqualTo(1);
        assertThat(duplicates).isEqualTo(3);
        assertThat(documentRepository.findNonArchivedByKnowledgeBaseIdAndContentHash(
                        DEMO_KB_ID, contentHash, KnowledgeDocumentStatus.ARCHIVED))
                .hasSize(1);
    }
}
