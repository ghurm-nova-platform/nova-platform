package ai.nova.platform.modelgateway.provider;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class AiModelProviderRegistry {

    private final Map<String, AiModelProvider> providersByKey;

    public AiModelProviderRegistry(List<AiModelProvider> providers) {
        Map<String, AiModelProvider> map = new LinkedHashMap<>();
        for (AiModelProvider provider : providers) {
            String key = provider.adapterKey();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException(
                        "Model provider returned blank adapter key: " + provider.getClass().getName());
            }
            if (map.containsKey(key)) {
                throw new IllegalStateException("Duplicate model provider adapter key: " + key);
            }
            map.put(key, provider);
        }
        this.providersByKey = Collections.unmodifiableMap(map);
    }

    public Optional<AiModelProvider> find(String adapterKey) {
        return Optional.ofNullable(providersByKey.get(adapterKey));
    }

    public AiModelProvider require(String adapterKey) {
        return find(adapterKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown model provider adapter: " + adapterKey));
    }

    public Set<String> allowedKeys() {
        return providersByKey.keySet();
    }

    public List<AiModelProvider> list() {
        return List.copyOf(providersByKey.values());
    }

    public boolean isRegistered(String adapterKey) {
        return providersByKey.containsKey(adapterKey);
    }
}
