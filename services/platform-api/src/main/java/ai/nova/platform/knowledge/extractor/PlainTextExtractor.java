package ai.nova.platform.knowledge.extractor;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentType;

@Component
public class PlainTextExtractor implements DocumentTextExtractor {

    private final KnowledgeProperties properties;

    public PlainTextExtractor(KnowledgeProperties properties) {
        this.properties = properties;
    }

    @Override
    public KnowledgeDocumentType supportedType() {
        return KnowledgeDocumentType.TEXT;
    }

    @Override
    public String extract(byte[] content, String fileName, String mediaType) {
        String text = decode(content);
        return TextNormalization.normalizeAndValidate(text, properties);
    }

    private static String decode(byte[] content) {
        Charset charset = StandardCharsets.UTF_8;
        return new String(content, charset);
    }
}
