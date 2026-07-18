package ai.nova.platform.knowledge.ingestion;

import java.time.Instant;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentRepository;

@Service
public class KnowledgeDocumentFailureService {

    private final KnowledgeDocumentRepository documentRepository;

    public KnowledgeDocumentFailureService(KnowledgeDocumentRepository documentRepository) {
        this.documentRepository = documentRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markFailed(UUID documentId, UUID actorId, String code) {
        documentRepository.findById(documentId).ifPresent(document -> {
            document.setStatus(KnowledgeDocumentStatus.FAILED);
            document.setIngestionErrorCode(code);
            document.setUpdatedBy(actorId);
            document.setUpdatedAt(Instant.now());
            documentRepository.saveAndFlush(document);
        });
    }
}
