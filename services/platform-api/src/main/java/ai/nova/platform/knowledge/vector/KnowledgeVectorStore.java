package ai.nova.platform.knowledge.vector;

import java.util.List;
import java.util.UUID;

public interface KnowledgeVectorStore {

    List<VectorSearchHit> search(
            UUID organizationId,
            UUID projectId,
            UUID knowledgeBaseId,
            String providerKey,
            String model,
            float[] queryEmbedding,
            int topK,
            double minimumScore,
            int maxCandidates);
}
