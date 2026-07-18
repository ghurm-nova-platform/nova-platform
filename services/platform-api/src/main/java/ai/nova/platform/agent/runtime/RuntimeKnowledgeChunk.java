package ai.nova.platform.agent.runtime;

import java.util.UUID;

public record RuntimeKnowledgeChunk(
        String label,
        UUID chunkId,
        UUID knowledgeBaseId,
        UUID documentId,
        int chunkIndex,
        String content,
        double score) {
}
