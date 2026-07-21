package ai.nova.platform.dashboard.service;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import ai.nova.platform.dashboard.config.DashboardProperties;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardSnapshot;

@Service
public class DashboardCacheService {

    private final DashboardProperties properties;
    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public DashboardCacheService(DashboardProperties properties) {
        this.properties = properties;
    }

    public Optional<CachedSnapshot> get(UUID organizationId, UUID projectId) {
        if (!properties.isEnabled() || !properties.getCache().isEnabled()) {
            return Optional.empty();
        }
        CacheKey key = new CacheKey(organizationId, projectId);
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            if (entry != null) {
                cache.remove(key, entry);
            }
            return Optional.empty();
        }
        return Optional.of(new CachedSnapshot(entry.snapshot(), entry.expiresAt(), true));
    }

    public CachedSnapshot put(UUID organizationId, UUID projectId, DashboardSnapshot snapshot) {
        Instant expiresAt = Instant.now().plusSeconds(Math.max(properties.getCache().getTtlSeconds(), 1));
        CacheEntry entry = new CacheEntry(snapshot, expiresAt);
        cache.put(new CacheKey(organizationId, projectId), entry);
        return new CachedSnapshot(snapshot, expiresAt, false);
    }

    public void invalidate(UUID organizationId, UUID projectId) {
        cache.remove(new CacheKey(organizationId, projectId));
    }

    public void invalidateOrganization(UUID organizationId) {
        cache.keySet().removeIf(key -> key.organizationId().equals(organizationId));
    }

    public int size() {
        return cache.size();
    }

    public record CachedSnapshot(DashboardSnapshot snapshot, Instant expiresAt, boolean fromCache) {}

    private record CacheKey(UUID organizationId, UUID projectId) {}

    private record CacheEntry(DashboardSnapshot snapshot, Instant expiresAt) {}
}
