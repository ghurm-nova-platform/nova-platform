package ai.nova.platform.knowledge.embedding;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.springframework.stereotype.Component;

@Component
public class EmbeddingProviderRegistry {

    private final Map<String, EmbeddingProvider> providersByKey;

    public EmbeddingProviderRegistry(List<EmbeddingProvider> providers) {
        Map<String, EmbeddingProvider> map = new LinkedHashMap<>();
        for (EmbeddingProvider provider : providers) {
            String key = provider.providerKey();
            if (key == null || key.isBlank()) {
                throw new IllegalStateException(
                        "Embedding provider returned blank key: " + provider.getClass().getName());
            }
            if (map.containsKey(key)) {
                throw new IllegalStateException("Duplicate embedding provider key: " + key);
            }
            map.put(key, provider);
        }
        this.providersByKey = Collections.unmodifiableMap(map);
    }

    public Optional<EmbeddingProvider> find(String providerKey) {
        return Optional.ofNullable(providersByKey.get(providerKey));
    }

    public EmbeddingProvider require(String providerKey) {
        return find(providerKey)
                .orElseThrow(() -> new IllegalArgumentException("Unknown embedding provider: " + providerKey));
    }

    public Set<String> allowedKeys() {
        return providersByKey.keySet();
    }

    public boolean isRegistered(String providerKey) {
        return providersByKey.containsKey(providerKey);
    }

    public List<EmbeddingProvider> list() {
        return List.copyOf(providersByKey.values());
    }
}
