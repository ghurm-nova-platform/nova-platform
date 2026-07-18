package ai.nova.platform.knowledge.extractor;

import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentType;

@Component
public class MarkdownTextExtractor implements DocumentTextExtractor {

    private final KnowledgeProperties properties;

    public MarkdownTextExtractor(KnowledgeProperties properties) {
        this.properties = properties;
    }

    @Override
    public KnowledgeDocumentType supportedType() {
        return KnowledgeDocumentType.MARKDOWN;
    }

    @Override
    public String extract(byte[] content, String fileName, String mediaType) {
        String text = new String(content, StandardCharsets.UTF_8);
        return TextNormalization.normalizeAndValidate(text, properties);
    }
}
