package ai.nova.platform.modelcatalog.sync;

import java.util.List;
import java.util.Set;

import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelgateway.entity.AiModelType;

public record DiscoveredModelDescriptor(
        String providerModelId,
        String displayName,
        String modelFamily,
        String modelVersion,
        AiModelType modelType,
        Integer contextWindow,
        Integer maxOutputTokens,
        Set<AiModelCapability> capabilities) {

    public DiscoveredModelDescriptor {
        capabilities = capabilities == null ? Set.of() : Set.copyOf(capabilities);
    }

    public static DiscoveredModelDescriptor of(
            String providerModelId,
            String displayName,
            String modelFamily,
            AiModelType modelType,
            Integer contextWindow,
            Integer maxOutputTokens,
            List<AiModelCapability> capabilities) {
        return new DiscoveredModelDescriptor(
                providerModelId,
                displayName,
                modelFamily,
                null,
                modelType,
                contextWindow,
                maxOutputTokens,
                capabilities == null ? Set.of() : Set.copyOf(capabilities));
    }
}
