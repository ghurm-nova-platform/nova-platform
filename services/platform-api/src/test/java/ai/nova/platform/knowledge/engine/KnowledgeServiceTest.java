package ai.nova.platform.knowledge.engine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;

import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.CreateDocumentRequest;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentDetail;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentSummary;
import ai.nova.platform.knowledge.engine.entity.DocumentStatus;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineChunkRepository;
import ai.nova.platform.knowledge.engine.service.KnowledgeService;
import ai.nova.platform.knowledge.engine.support.KnowledgeEngineTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = {"nova.knowledge.engine.enabled=true", "nova.audit.enabled=true"})
class KnowledgeServiceTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private KnowledgeEngineChunkRepository chunkRepository;

    @Test
    void createListGetUpdateAndArchiveRestore() {
        String title = KnowledgeEngineTestFixture.uniqueTitle("svc-doc");
        CreateDocumentRequest request = KnowledgeEngineTestFixture.sampleCreateRequest(title);

        DocumentDetail created = knowledgeService.create(request, KnowledgeEngineTestFixture.knowledgeWriteUser());
        assertThat(created.id()).isNotNull();
        assertThat(created.title()).isEqualTo(title);
        assertThat(created.status()).isEqualTo(DocumentStatus.ACTIVE);
        assertThat(chunkRepository.findByDocumentIdOrderByChunkNumberAsc(created.id())).isNotEmpty();

        var summaries = knowledgeService.list(KnowledgeEngineTestFixture.PROJECT_ID, KnowledgeEngineTestFixture.knowledgeReadUser());
        assertThat(summaries.stream().map(DocumentSummary::id)).contains(created.id());

        DocumentDetail loaded = knowledgeService.get(created.id(), KnowledgeEngineTestFixture.knowledgeReadUser());
        assertThat(loaded.content()).contains("searchable keyword alpha");

        DocumentDetail archived = knowledgeService.archive(created.id(), KnowledgeEngineTestFixture.knowledgeAdminUser());
        assertThat(archived.status()).isEqualTo(DocumentStatus.ARCHIVED);

        DocumentDetail restored = knowledgeService.restore(created.id(), KnowledgeEngineTestFixture.knowledgeAdminUser());
        assertThat(restored.status()).isEqualTo(DocumentStatus.ACTIVE);
    }

    @Test
    void privateDocumentsAreHiddenFromOtherReaders() {
        String title = KnowledgeEngineTestFixture.uniqueTitle("private-doc");
        DocumentDetail created = knowledgeService.create(
                KnowledgeEngineTestFixture.privateCreateRequest(title),
                KnowledgeEngineTestFixture.knowledgeWriteUser());

        assertThatThrownBy(() -> knowledgeService.get(created.id(), KnowledgeEngineTestFixture.privateDocumentReader()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
