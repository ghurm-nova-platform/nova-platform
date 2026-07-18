package ai.nova.platform.knowledge.embedding;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DeterministicLocalEmbeddingProviderTest {

    private final DeterministicLocalEmbeddingProvider provider = new DeterministicLocalEmbeddingProvider();

    @Test
    void producesDeterministicNormalizedFiniteVectors() {
        float[] a = provider.embed("hello knowledge");
        float[] b = provider.embed("hello knowledge");
        float[] c = provider.embed("different text");

        assertThat(provider.providerKey()).isEqualTo("DETERMINISTIC_LOCAL");
        assertThat(provider.model()).isEqualTo("deterministic-v1");
        assertThat(a).hasSize(64);
        assertThat(a).containsExactly(b);
        assertThat(a).isNotEqualTo(c);

        double norm = 0.0;
        for (float v : a) {
            assertThat(Float.isFinite(v)).isTrue();
            norm += (double) v * v;
        }
        assertThat(Math.sqrt(norm)).isCloseTo(1.0, org.assertj.core.data.Offset.offset(1e-5));
    }
}
