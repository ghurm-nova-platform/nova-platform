package ai.nova.platform.knowledge.engine.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.knowledge.engine.config.KnowledgeEngineProperties;
import ai.nova.platform.knowledge.engine.entity.KnowledgeChunkEntity;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineChunkRepository;

@Service
public class KnowledgeIndexService {

    private final KnowledgeEngineProperties properties;
    private final KnowledgeEngineChunkRepository chunkRepository;

    public KnowledgeIndexService(KnowledgeEngineProperties properties, KnowledgeEngineChunkRepository chunkRepository) {
        this.properties = properties;
        this.chunkRepository = chunkRepository;
    }

    @Transactional
    public void reindexDocument(UUID documentId, UUID organizationId, String content, Instant now) {
        chunkRepository.deleteByDocumentId(documentId);
        if (content == null || content.isBlank()) {
            return;
        }
        int chunkSize = Math.max(properties.getChunkSize(), 1);
        int overlap = Math.max(0, Math.min(properties.getChunkOverlap(), chunkSize - 1));
        int step = Math.max(1, chunkSize - overlap);
        List<KnowledgeChunkEntity> chunks = new ArrayList<>();
        int chunkNumber = 0;
        for (int start = 0; start < content.length(); start += step) {
            int end = Math.min(content.length(), start + chunkSize);
            String piece = content.substring(start, end);
            chunks.add(new KnowledgeChunkEntity(
                    UUID.randomUUID(),
                    documentId,
                    organizationId,
                    chunkNumber++,
                    start,
                    end,
                    piece,
                    now));
            if (end >= content.length()) {
                break;
            }
        }
        chunkRepository.saveAll(chunks);
    }
}
