package ai.nova.platform.agent.runtime;

import java.util.List;

public record RuntimeKnowledgeContext(
        List<RuntimeKnowledgeCitation> citations,
        List<RuntimeKnowledgeChunk> chunks) {

    public RuntimeKnowledgeContext {
        citations = citations == null ? List.of() : List.copyOf(citations);
        chunks = chunks == null ? List.of() : List.copyOf(chunks);
    }

    public static RuntimeKnowledgeContext empty() {
        return new RuntimeKnowledgeContext(List.of(), List.of());
    }

    public boolean isEmpty() {
        return chunks.isEmpty();
    }
}
