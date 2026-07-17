package ai.nova.platform.agent.runtime;

import java.util.UUID;

public record RuntimeKnowledgeCitation(
        String label,
        UUID knowledgeBaseId,
        String knowledgeBaseName,
        UUID documentId,
        String documentName,
        int chunkIndex,
        double score) {
}
