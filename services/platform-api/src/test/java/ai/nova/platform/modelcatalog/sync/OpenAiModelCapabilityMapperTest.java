package ai.nova.platform.modelcatalog.sync;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;

import org.junit.jupiter.api.Test;

import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelgateway.entity.AiModelType;

class OpenAiModelCapabilityMapperTest {

    private final OpenAiModelCapabilityMapper mapper = new OpenAiModelCapabilityMapper();

    @Test
    void mapsKnownGpt4oFamily() {
        Set<AiModelCapability> caps = mapper.mapCapabilities("gpt-4o-mini");
        assertThat(caps)
                .contains(
                        AiModelCapability.CHAT,
                        AiModelCapability.TOOL_CALLING,
                        AiModelCapability.FUNCTION_CALLING,
                        AiModelCapability.JSON_MODE,
                        AiModelCapability.VISION,
                        AiModelCapability.STREAMING);
        assertThat(mapper.mapType("gpt-4o-mini")).isEqualTo(AiModelType.MULTIMODAL);
        assertThat(mapper.mapFamily("gpt-4o-mini")).isEqualTo("gpt-4o");
        assertThat(mapper.mapContextWindow("gpt-4o-mini")).isEqualTo(128_000);
    }

    @Test
    void mapsEmbeddingsOnly() {
        assertThat(mapper.mapCapabilities("text-embedding-3-large"))
                .containsExactly(AiModelCapability.EMBEDDINGS);
        assertThat(mapper.mapType("text-embedding-3-large")).isEqualTo(AiModelType.EMBEDDING);
    }

    @Test
    void unknownIdsGetNoCapabilities() {
        assertThat(mapper.mapCapabilities("ft:custom-unknown-model")).isEmpty();
        assertThat(mapper.mapFamily("ft:custom-unknown-model")).isNull();
        assertThat(mapper.mapContextWindow("ft:custom-unknown-model")).isNull();
    }
}
