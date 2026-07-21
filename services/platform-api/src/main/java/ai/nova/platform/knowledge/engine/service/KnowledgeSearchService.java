package ai.nova.platform.knowledge.engine.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.knowledge.engine.config.KnowledgeEngineProperties;
import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.SearchResult;
import ai.nova.platform.knowledge.engine.entity.Category;
import ai.nova.platform.knowledge.engine.entity.DocumentStatus;
import ai.nova.platform.knowledge.engine.entity.KnowledgeDocumentEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeTagEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.entity.Visibility;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineDocumentRepository;
import ai.nova.platform.knowledge.engine.security.KnowledgeEngineAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class KnowledgeSearchService {

    private final KnowledgeEngineProperties properties;
    private final KnowledgeEngineAuthorizationService authorizationService;
    private final KnowledgeVisibilityService visibilityService;
    private final KnowledgeEngineDocumentRepository documentRepository;
    private final ConcurrentHashMap<CacheKey, CacheEntry> cache = new ConcurrentHashMap<>();

    public KnowledgeSearchService(
            KnowledgeEngineProperties properties,
            KnowledgeEngineAuthorizationService authorizationService,
            KnowledgeVisibilityService visibilityService,
            KnowledgeEngineDocumentRepository documentRepository) {
        this.properties = properties;
        this.authorizationService = authorizationService;
        this.visibilityService = visibilityService;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<SearchResult> search(
            AuthenticatedUser user,
            String query,
            String tag,
            Category category,
            UUID projectId,
            UUID authorId,
            Visibility visibility,
            KnowledgeType knowledgeType,
            Instant fromDate,
            Instant toDate) {
        authorizationService.requireRead(user);
        CacheKey cacheKey = new CacheKey(
                user.getOrganizationId(),
                user.getUserId(),
                query,
                tag,
                category,
                projectId,
                authorId,
                visibility,
                knowledgeType,
                fromDate,
                toDate);
        Optional<List<SearchResult>> cached = getCached(cacheKey);
        if (cached.isPresent()) {
            return cached.get();
        }

        List<KnowledgeDocumentEntity> documents = documentRepository.searchDocuments(
                user.getOrganizationId(),
                DocumentStatus.DELETED,
                projectId,
                category,
                knowledgeType,
                authorId,
                tag,
                fromDate,
                toDate,
                query);

        if (query != null && !query.isBlank()) {
            Map<UUID, KnowledgeDocumentEntity> merged = new LinkedHashMap<>();
            documents.forEach(doc -> merged.put(doc.getId(), doc));
            documentRepository
                    .searchByChunkContent(user.getOrganizationId(), DocumentStatus.DELETED, query)
                    .forEach(doc -> merged.putIfAbsent(doc.getId(), doc));
            documents = new ArrayList<>(merged.values());
        }

        List<SearchResult> results = documents.stream()
                .filter(doc -> visibilityService.canRead(user, doc))
                .filter(doc -> visibility == null || doc.getVisibility() == visibility)
                .map(doc -> toSearchResult(doc, query))
                .collect(Collectors.toList());

        putCache(cacheKey, results);
        return results;
    }

    public void invalidateOrganization(UUID organizationId) {
        cache.keySet().removeIf(key -> key.organizationId().equals(organizationId));
    }

    private SearchResult toSearchResult(KnowledgeDocumentEntity doc, String query) {
        String snippet = doc.getSummary();
        if (query != null && !query.isBlank() && doc.getContent() != null) {
            String lowerContent = doc.getContent().toLowerCase();
            String lowerQuery = query.toLowerCase();
            int index = lowerContent.indexOf(lowerQuery);
            if (index >= 0) {
                int start = Math.max(0, index - 40);
                int end = Math.min(doc.getContent().length(), index + query.length() + 40);
                snippet = doc.getContent().substring(start, end);
            }
        }
        return new SearchResult(
                doc.getId(),
                doc.getTitle(),
                snippet,
                doc.getKnowledgeType(),
                doc.getCategory(),
                doc.getVisibility(),
                doc.getProjectId(),
                doc.getAuthorId(),
                tagNames(doc),
                snippet,
                doc.getUpdatedAt());
    }

    private List<String> tagNames(KnowledgeDocumentEntity doc) {
        return doc.getTags().stream().map(KnowledgeTagEntity::getName).sorted().toList();
    }

    private Optional<List<SearchResult>> getCached(CacheKey key) {
        if (!properties.isEnabled() || !properties.getCache().isEnabled()) {
            return Optional.empty();
        }
        CacheEntry entry = cache.get(key);
        if (entry == null || entry.expiresAt().isBefore(Instant.now())) {
            if (entry != null) {
                cache.remove(key, entry);
            }
            return Optional.empty();
        }
        return Optional.of(entry.results());
    }

    private void putCache(CacheKey key, List<SearchResult> results) {
        if (!properties.isEnabled() || !properties.getCache().isEnabled()) {
            return;
        }
        Instant expiresAt = Instant.now().plusSeconds(Math.max(properties.getCache().getTtlSeconds(), 1));
        cache.put(key, new CacheEntry(results, expiresAt));
    }

    private record CacheKey(
            UUID organizationId,
            UUID userId,
            String query,
            String tag,
            Category category,
            UUID projectId,
            UUID authorId,
            Visibility visibility,
            KnowledgeType knowledgeType,
            Instant fromDate,
            Instant toDate) {}

    private record CacheEntry(List<SearchResult> results, Instant expiresAt) {}
}
