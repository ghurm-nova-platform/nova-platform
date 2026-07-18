package ai.nova.platform.knowledge.embedding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

class EmbeddingProviderRegistryTest {

    @Test
    void registersProvidersAndFailsOnDuplicateKeys() {
        EmbeddingProviderRegistry registry = new EmbeddingProviderRegistry(List.of(new DeterministicLocalEmbeddingProvider()));
        assertThat(registry.isRegistered("DETERMINISTIC_LOCAL")).isTrue();
        assertThat(registry.require("DETERMINISTIC_LOCAL").dimensions()).isEqualTo(64);

        assertThatThrownBy(() -> new EmbeddingProviderRegistry(List.of(
                        new DeterministicLocalEmbeddingProvider(), new DeterministicLocalEmbeddingProvider())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Duplicate embedding provider key");
    }
}
