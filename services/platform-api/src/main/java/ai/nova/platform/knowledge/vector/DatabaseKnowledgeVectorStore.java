package ai.nova.platform.knowledge.vector;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.knowledge.embedding.EmbeddingCodec;
import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.entity.KnowledgeChunk;
import ai.nova.platform.knowledge.entity.KnowledgeDocument;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.entity.KnowledgeEmbedding;
import ai.nova.platform.knowledge.repository.KnowledgeBaseRepository;
import ai.nova.platform.knowledge.repository.KnowledgeChunkRepository;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeEmbeddingRepository;

@Component
public class DatabaseKnowledgeVectorStore implements KnowledgeVectorStore {

    private final KnowledgeEmbeddingRepository embeddingRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;

    public DatabaseKnowledgeVectorStore(
            KnowledgeEmbeddingRepository embeddingRepository,
            KnowledgeChunkRepository chunkRepository,
            KnowledgeDocumentRepository documentRepository,
            KnowledgeBaseRepository knowledgeBaseRepository) {
        this.embeddingRepository = embeddingRepository;
        this.chunkRepository = chunkRepository;
        this.documentRepository = documentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<VectorSearchHit> search(
            UUID organizationId,
            UUID projectId,
            UUID knowledgeBaseId,
            String providerKey,
            String model,
            float[] queryEmbedding,
            int topK,
            double minimumScore,
            int maxCandidates) {
        KnowledgeBase kb = knowledgeBaseRepository
                .findByIdAndProjectIdAndOrganizationId(knowledgeBaseId, projectId, organizationId)
                .orElse(null);
        if (kb == null || kb.getStatus() != KnowledgeBaseStatus.ACTIVE) {
            return List.of();
        }

        List<KnowledgeEmbedding> embeddings = embeddingRepository.findCandidates(
                organizationId, projectId, knowledgeBaseId, providerKey, model);
        if (embeddings.isEmpty()) {
            return List.of();
        }

        Set<UUID> documentIds = embeddings.stream().map(KnowledgeEmbedding::getDocumentId).collect(Collectors.toSet());
        Map<UUID, KnowledgeDocument> documents = documentRepository
                .findByIdInAndProjectIdAndOrganizationId(List.copyOf(documentIds), projectId, organizationId)
                .stream()
                .filter(doc -> doc.getStatus() == KnowledgeDocumentStatus.READY)
                .collect(Collectors.toMap(KnowledgeDocument::getId, doc -> doc));

        Set<UUID> readyDocIds = documents.keySet();
        List<KnowledgeEmbedding> eligible = embeddings.stream()
                .filter(e -> readyDocIds.contains(e.getDocumentId()))
                .limit(Math.max(1, maxCandidates))
                .toList();

        Set<UUID> chunkIds = eligible.stream().map(KnowledgeEmbedding::getChunkId).collect(Collectors.toCollection(HashSet::new));
        Map<UUID, KnowledgeChunk> chunks = chunkRepository
                .findByIdInAndProjectIdAndOrganizationId(List.copyOf(chunkIds), projectId, organizationId)
                .stream()
                .collect(Collectors.toMap(KnowledgeChunk::getId, c -> c, (a, b) -> a, HashMap::new));

        List<VectorSearchHit> hits = new ArrayList<>();
        for (KnowledgeEmbedding embedding : eligible) {
            KnowledgeChunk chunk = chunks.get(embedding.getChunkId());
            if (chunk == null) {
                continue;
            }
            float[] vector = EmbeddingCodec.decode(embedding.getEmbedding());
            double score = cosineSimilarity(queryEmbedding, vector);
            if (score < minimumScore) {
                continue;
            }
            hits.add(new VectorSearchHit(
                    chunk.getId(),
                    chunk.getDocumentId(),
                    knowledgeBaseId,
                    score,
                    chunk.getContentHash()));
        }

        hits.sort(Comparator.comparingDouble(VectorSearchHit::score)
                .reversed()
                .thenComparing(VectorSearchHit::chunkId));
        if (hits.size() > topK) {
            return List.copyOf(hits.subList(0, topK));
        }
        return List.copyOf(hits);
    }

    static double cosineSimilarity(float[] left, float[] right) {
        if (left.length != right.length) {
            return -1.0;
        }
        double dot = 0.0;
        double leftNorm = 0.0;
        double rightNorm = 0.0;
        for (int i = 0; i < left.length; i++) {
            dot += (double) left[i] * right[i];
            leftNorm += (double) left[i] * left[i];
            rightNorm += (double) right[i] * right[i];
        }
        if (leftNorm == 0.0 || rightNorm == 0.0) {
            return 0.0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }
}
