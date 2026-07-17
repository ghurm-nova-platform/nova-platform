package ai.nova.platform.knowledge.extractor;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import ai.nova.platform.knowledge.config.KnowledgeProperties;

class MarkdownTextExtractorTest {

    @Test
    void extractsMarkdownText() {
        MarkdownTextExtractor extractor = new MarkdownTextExtractor(new KnowledgeProperties());
        String text = extractor.extract("# Title\n\nBody".getBytes(StandardCharsets.UTF_8), "a.md", "text/markdown");
        assertThat(text).contains("Title").contains("Body");
    }
}
