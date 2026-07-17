package ai.nova.platform.knowledge.vector;

import java.util.UUID;

public record VectorSearchHit(
        UUID chunkId,
        UUID documentId,
        UUID knowledgeBaseId,
        double score,
        String contentHash) {
}
