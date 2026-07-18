package ai.nova.platform.knowledge.extractor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentType;
import ai.nova.platform.web.error.ApiException;

@Component
public class DocumentTextExtractorRegistry {

    private final Map<KnowledgeDocumentType, DocumentTextExtractor> extractors;
    private final KnowledgeProperties properties;

    public DocumentTextExtractorRegistry(List<DocumentTextExtractor> extractors, KnowledgeProperties properties) {
        Map<KnowledgeDocumentType, DocumentTextExtractor> map = new EnumMap<>(KnowledgeDocumentType.class);
        for (DocumentTextExtractor extractor : extractors) {
            if (map.containsKey(extractor.supportedType())) {
                throw new IllegalStateException("Duplicate extractor for " + extractor.supportedType());
            }
            map.put(extractor.supportedType(), extractor);
        }
        this.extractors = Map.copyOf(map);
        this.properties = properties;
    }

    public DocumentTextExtractor require(KnowledgeDocumentType type) {
        if (type == KnowledgeDocumentType.PDF && !properties.isPdfEnabled()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DOCUMENT_TYPE_UNSUPPORTED",
                    "PDF documents are not supported in this phase");
        }
        DocumentTextExtractor extractor = extractors.get(type);
        if (extractor == null) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "DOCUMENT_TYPE_UNSUPPORTED",
                    "Document type is not supported: " + type);
        }
        return extractor;
    }
}
