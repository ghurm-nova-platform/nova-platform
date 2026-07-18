package ai.nova.platform.knowledge.chunking;

public record TextChunk(int index, String content, int characterStart, int characterEnd, String contentHash) {
}
