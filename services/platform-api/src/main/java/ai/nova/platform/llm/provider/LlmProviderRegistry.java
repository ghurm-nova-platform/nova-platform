package ai.nova.platform.llm.provider;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Component;

import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.web.error.ApiException;
import org.springframework.http.HttpStatus;

@Component
public class LlmProviderRegistry {

    private final Map<LlmProviderType, LlmRuntimeProvider> providers = new EnumMap<>(LlmProviderType.class);

    public LlmProviderRegistry(List<LlmRuntimeProvider> providerList) {
        for (LlmRuntimeProvider provider : providerList) {
            providers.put(provider.providerType(), provider);
        }
    }

    public Optional<LlmRuntimeProvider> find(LlmProviderType type) {
        return Optional.ofNullable(providers.get(type));
    }

    public LlmRuntimeProvider require(LlmProviderType type) {
        return find(type)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.BAD_REQUEST, LlmErrorCodes.PROVIDER_UNAVAILABLE, "Unknown provider: " + type));
    }

    public List<LlmRuntimeProvider> list() {
        return List.copyOf(providers.values());
    }
}
