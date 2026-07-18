package ai.nova.platform.knowledge.chunking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import ai.nova.platform.knowledge.config.KnowledgeProperties;

class ParagraphAwareTextChunkerTest {

    private final ParagraphAwareTextChunker chunker = new ParagraphAwareTextChunker(new KnowledgeProperties());

    @Test
    void chunksDeterministicallyByParagraphThenOverlap() {
        String text = "Alpha paragraph one.\n\nBeta paragraph two is longer.\n\nGamma paragraph three.";
        List<TextChunk> first = chunker.chunk(text, 40, 10);
        List<TextChunk> second = chunker.chunk(text, 40, 10);

        assertThat(first).isNotEmpty();
        assertThat(first).hasSameSizeAs(second);
        for (int i = 0; i < first.size(); i++) {
            assertThat(first.get(i).index()).isEqualTo(i);
            assertThat(first.get(i).content()).isEqualTo(second.get(i).content());
            assertThat(first.get(i).contentHash()).isEqualTo(second.get(i).contentHash());
            assertThat(first.get(i).content()).isNotBlank();
        }
    }

    @Test
    void rejectsEmptyOnlyAfterTrim() {
        assertThat(chunker.chunk("   \n\n  ", 100, 10)).isEmpty();
    }
}
