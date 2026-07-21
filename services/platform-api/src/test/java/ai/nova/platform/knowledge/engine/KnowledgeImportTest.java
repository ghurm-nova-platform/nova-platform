package ai.nova.platform.knowledge.engine;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.DocumentDetail;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.service.KnowledgeImportExportService;
import ai.nova.platform.knowledge.engine.support.KnowledgeEngineTestFixture;

@SpringBootTest(properties = {"nova.knowledge.engine.enabled=true", "nova.audit.enabled=true"})
class KnowledgeImportTest {

    @Autowired
    private KnowledgeImportExportService importExportService;

    @Test
    void importAdrDocumentAndExportFormats() {
        String title = KnowledgeEngineTestFixture.uniqueTitle("adr-import");
        DocumentDetail imported = importExportService.importDocument(
                KnowledgeEngineTestFixture.adrImportRequest(title), KnowledgeEngineTestFixture.knowledgeWriteUser());

        assertThat(imported.knowledgeType()).isEqualTo(KnowledgeType.ADR);
        assertThat(imported.content()).contains("PostgreSQL");

        var markdown = importExportService.exportDocument(imported, "markdown");
        assertThat(new String(markdown.content())).contains(title);

        var json = importExportService.exportDocument(imported, "json");
        assertThat(new String(json.content())).contains("ADR");

        var pdf = importExportService.exportDocument(imported, "pdf");
        assertThat(pdf.content()).isNotEmpty();
        assertThat(pdf.contentType()).isEqualTo("application/pdf");
    }
}
