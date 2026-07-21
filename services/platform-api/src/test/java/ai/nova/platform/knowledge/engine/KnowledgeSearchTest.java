package ai.nova.platform.knowledge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentDetail;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.SearchResult;
import ai.nova.platform.knowledge.engine.service.KnowledgeSearchService;
import ai.nova.platform.knowledge.engine.service.KnowledgeService;
import ai.nova.platform.knowledge.engine.support.KnowledgeEngineTestFixture;

@SpringBootTest(properties = {"nova.knowledge.engine.enabled=true", "nova.audit.enabled=true"})
class KnowledgeSearchTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Autowired
    private KnowledgeSearchService searchService;

    @Test
    void searchFindsDocumentsByTitleContentAndTag() {
        String title = KnowledgeEngineTestFixture.uniqueTitle("search-doc");
        DocumentDetail created = knowledgeService.create(
                KnowledgeEngineTestFixture.sampleCreateRequest(title),
                KnowledgeEngineTestFixture.knowledgeWriteUser());

        var byTitle = searchService.search(
                KnowledgeEngineTestFixture.knowledgeReadUser(),
                title,
                null,
                null,
                KnowledgeEngineTestFixture.PROJECT_ID,
                null,
                null,
                null,
                null,
                null);
        assertThat(byTitle.stream().map(SearchResult::id)).contains(created.id());

        var byContent = searchService.search(
                KnowledgeEngineTestFixture.knowledgeReadUser(),
                "searchable keyword alpha",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        assertThat(byContent.stream().map(SearchResult::id)).contains(created.id());

        var byTag = searchService.search(
                KnowledgeEngineTestFixture.knowledgeReadUser(),
                null,
                "platform",
                null,
                null,
                null,
                null,
                null,
                null,
                null);
        assertThat(byTag.stream().map(SearchResult::id)).contains(created.id());
    }
}
