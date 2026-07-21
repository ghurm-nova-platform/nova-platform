package ai.nova.platform.llm.service;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import ai.nova.platform.llm.configuration.LlmProperties;
import ai.nova.platform.llm.provider.LlmCompletionResult;

@Service
public class LlmCacheService {

    private final LlmProperties properties;
    private final ConcurrentHashMap<String, CacheEntry> cache = new ConcurrentHashMap<>();

    public LlmCacheService(LlmProperties properties) {
        this.properties = properties;
    }

    public Optional<LlmCompletionResult> get(String key) {
        if (!properties.getCache().isEnabled() || key == null) {
            return Optional.empty();
        }
        CacheEntry entry = cache.get(key);
        if (entry == null) {
            return Optional.empty();
        }
        if (entry.expiresAt().isBefore(Instant.now())) {
            cache.remove(key);
            return Optional.empty();
        }
        return Optional.of(entry.result());
    }

    public void put(String key, LlmCompletionResult result) {
        if (!properties.getCache().isEnabled() || key == null || result == null) {
            return;
        }
        Instant expires = Instant.now().plusSeconds(Math.max(1, properties.getCache().getTtlSeconds()));
        cache.put(key, new CacheEntry(result, expires));
    }

    public void clear() {
        cache.clear();
    }

    private record CacheEntry(LlmCompletionResult result, Instant expiresAt) {
    }
}
