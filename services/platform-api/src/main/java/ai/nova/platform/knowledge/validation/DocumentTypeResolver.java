package ai.nova.platform.knowledge.validation;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentType;
import ai.nova.platform.web.error.ApiException;

@Component
public class DocumentTypeResolver {

    private final KnowledgeProperties properties;

    public DocumentTypeResolver(KnowledgeProperties properties) {
        this.properties = properties;
    }

    public KnowledgeDocumentType resolve(String fileName, String mediaType) {
        String name = fileName == null ? "" : fileName.toLowerCase();
        String mime = mediaType == null ? "" : mediaType.toLowerCase();

        if (mime.contains("pdf") || name.endsWith(".pdf")) {
            if (!properties.isPdfEnabled()) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "DOCUMENT_TYPE_UNSUPPORTED",
                        "PDF documents are not supported in this phase");
            }
            return KnowledgeDocumentType.PDF;
        }
        if (mime.equals("text/markdown")
                || mime.equals("text/x-markdown")
                || name.endsWith(".md")
                || name.endsWith(".markdown")) {
            return KnowledgeDocumentType.MARKDOWN;
        }
        if (mime.equals("text/plain") || name.endsWith(".txt") || mime.startsWith("text/")) {
            if (mime.contains("html") || mime.contains("xml") || name.endsWith(".html") || name.endsWith(".xml")) {
                throw new ApiException(
                        HttpStatus.BAD_REQUEST,
                        "DOCUMENT_TYPE_UNSUPPORTED",
                        "Document type is not supported");
            }
            return KnowledgeDocumentType.TEXT;
        }
        if (!StringUtils.hasText(mime) && name.endsWith(".txt")) {
            return KnowledgeDocumentType.TEXT;
        }
        throw new ApiException(
                HttpStatus.BAD_REQUEST, "DOCUMENT_TYPE_UNSUPPORTED", "Document type is not supported");
    }
}
