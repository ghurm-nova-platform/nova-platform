package ai.nova.platform.modelgateway.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class AiModelProviderRegistryTest {

    @Test
    void registersDeterministicProvider() {
        AiModelProviderRegistry registry =
                new AiModelProviderRegistry(List.of(new DeterministicLocalModelProvider(new ObjectMapper())));
        assertThat(registry.allowedKeys()).contains(DeterministicLocalModelProvider.ADAPTER_KEY);
    }

    @Test
    void failsOnDuplicateAdapterKey() {
        DeterministicLocalModelProvider provider = new DeterministicLocalModelProvider(new ObjectMapper());
        assertThatThrownBy(() -> new AiModelProviderRegistry(List.of(provider, provider)))
                .isInstanceOf(IllegalStateException.class);
    }
}
