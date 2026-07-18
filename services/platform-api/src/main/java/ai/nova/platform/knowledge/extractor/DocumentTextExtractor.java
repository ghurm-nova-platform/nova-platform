package ai.nova.platform.knowledge.extractor;

import ai.nova.platform.knowledge.entity.KnowledgeDocumentType;

public interface DocumentTextExtractor {

    KnowledgeDocumentType supportedType();

    String extract(byte[] content, String fileName, String mediaType);
}
