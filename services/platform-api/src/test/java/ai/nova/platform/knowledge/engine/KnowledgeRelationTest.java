package ai.nova.platform.knowledge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentDetail;
import ai.nova.platform.knowledge.engine.service.KnowledgeService;
import ai.nova.platform.knowledge.engine.support.KnowledgeEngineTestFixture;

@SpringBootTest(properties = {"nova.knowledge.engine.enabled=true", "nova.audit.enabled=true"})
class KnowledgeRelationTest {

    @Autowired
    private KnowledgeService knowledgeService;

    @Test
    void relateDocuments() {
        DocumentDetail source = knowledgeService.create(
                KnowledgeEngineTestFixture.sampleCreateRequest(KnowledgeEngineTestFixture.uniqueTitle("rel-source")),
                KnowledgeEngineTestFixture.knowledgeWriteUser());
        DocumentDetail target = knowledgeService.create(
                KnowledgeEngineTestFixture.sampleCreateRequest(KnowledgeEngineTestFixture.uniqueTitle("rel-target")),
                KnowledgeEngineTestFixture.knowledgeWriteUser());

        var relations = knowledgeService.relate(
                source.id(),
                KnowledgeEngineTestFixture.relateRequest(target.id()),
                KnowledgeEngineTestFixture.knowledgeWriteUser());

        assertThat(relations).hasSize(1);
        assertThat(relations.get(0).targetDocumentId()).isEqualTo(target.id());

        var loaded = knowledgeService.relations(source.id(), KnowledgeEngineTestFixture.knowledgeReadUser());
        assertThat(loaded).extracting(r -> r.targetDocumentId()).contains(target.id());
    }
}
