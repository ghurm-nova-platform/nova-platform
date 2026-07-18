package ai.nova.platform.knowledge.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.CreateKnowledgeBaseRequest;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.EmbeddingProviderResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.EmbeddingProvidersResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeBaseResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.UpdateKnowledgeBaseRequest;
import ai.nova.platform.knowledge.embedding.EmbeddingProvider;
import ai.nova.platform.knowledge.embedding.EmbeddingProviderRegistry;
import ai.nova.platform.knowledge.entity.AgentKnowledgeAssignment;
import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.mapper.KnowledgeMapper;
import ai.nova.platform.knowledge.repository.AgentKnowledgeAssignmentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeBaseRepository;
import ai.nova.platform.knowledge.security.KnowledgeAuthorizationService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class KnowledgeBaseService {

    private static final Pattern KNOWLEDGE_KEY_PATTERN = Pattern.compile("^[A-Z][A-Z0-9_]*$");

    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final AgentKnowledgeAssignmentRepository assignmentRepository;
    private final ProjectRepository projectRepository;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final KnowledgeProperties properties;
    private final KnowledgeMapper mapper;
    private final KnowledgeAuthorizationService authorizationService;

    public KnowledgeBaseService(
            KnowledgeBaseRepository knowledgeBaseRepository,
            AgentKnowledgeAssignmentRepository assignmentRepository,
            ProjectRepository projectRepository,
            EmbeddingProviderRegistry embeddingProviderRegistry,
            KnowledgeProperties properties,
            KnowledgeMapper mapper,
            KnowledgeAuthorizationService authorizationService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.assignmentRepository = assignmentRepository;
        this.projectRepository = projectRepository;
        this.embeddingProviderRegistry = embeddingProviderRegistry;
        this.properties = properties;
        this.mapper = mapper;
        this.authorizationService = authorizationService;
    }

    @Transactional(readOnly = true)
    public Page<KnowledgeBaseResponse> list(
            UUID projectId, KnowledgeBaseStatus status, String search, Pageable pageable, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_READ);
        requireProject(projectId, user.getOrganizationId());
        String normalizedSearch = search == null || search.isBlank() ? null : search.trim();
        return knowledgeBaseRepository
                .search(user.getOrganizationId(), projectId, status, normalizedSearch, pageable)
                .map(mapper::toBaseResponse);
    }

    @Transactional(readOnly = true)
    public KnowledgeBaseResponse get(UUID projectId, UUID id, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_READ);
        return mapper.toBaseResponse(requireKnowledgeBase(projectId, id, user.getOrganizationId()));
    }

    @Transactional(readOnly = true)
    public EmbeddingProvidersResponse listProviders(UUID projectId, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_READ);
        requireProject(projectId, user.getOrganizationId());
        List<EmbeddingProviderResponse> providers = embeddingProviderRegistry.list().stream()
                .map(p -> new EmbeddingProviderResponse(p.providerKey(), p.model(), p.dimensions()))
                .toList();
        return new EmbeddingProvidersResponse(providers);
    }

    @Transactional
    public KnowledgeBaseResponse create(UUID projectId, CreateKnowledgeBaseRequest request, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_CREATE);
        requireProject(projectId, user.getOrganizationId());

        String key = request.knowledgeKey().trim().toUpperCase();
        if (!KNOWLEDGE_KEY_PATTERN.matcher(key).matches()) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST,
                    "KNOWLEDGE_KEY_INVALID",
                    "knowledgeKey must be uppercase snake case");
        }
        if (knowledgeBaseRepository.existsByProjectIdAndKnowledgeKey(projectId, key)) {
            throw new ApiException(HttpStatus.CONFLICT, "KNOWLEDGE_KEY_EXISTS", "Knowledge key already exists");
        }

        EmbeddingProvider provider = requireProvider(request.embeddingProviderKey());
        int chunkSize = request.chunkSize() != null ? request.chunkSize() : properties.getDefaultChunkSize();
        int chunkOverlap =
                request.chunkOverlap() != null ? request.chunkOverlap() : properties.getDefaultChunkOverlap();
        validateChunkConfig(chunkSize, chunkOverlap);
        int topK = request.defaultTopK() != null ? request.defaultTopK() : properties.getDefaultTopK();
        if (topK < 1 || topK > properties.getMaximumTopK()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "defaultTopK out of range");
        }
        BigDecimal minimumScore = request.minimumScore() != null
                ? request.minimumScore()
                : BigDecimal.valueOf(properties.getDefaultMinimumScore());

        Instant now = Instant.now();
        KnowledgeBase kb = new KnowledgeBase(
                UUID.randomUUID(),
                user.getOrganizationId(),
                projectId,
                key,
                request.name().trim(),
                request.description(),
                KnowledgeBaseStatus.DRAFT,
                provider.providerKey(),
                provider.model(),
                provider.dimensions(),
                chunkSize,
                chunkOverlap,
                topK,
                minimumScore,
                user.getUserId(),
                now);
        return mapper.toBaseResponse(knowledgeBaseRepository.save(kb));
    }

    @Transactional
    public KnowledgeBaseResponse update(
            UUID projectId, UUID id, UpdateKnowledgeBaseRequest request, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_UPDATE);
        KnowledgeBase kb = requireKnowledgeBase(projectId, id, user.getOrganizationId());
        if (kb.getStatus() == KnowledgeBaseStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "KNOWLEDGE_ARCHIVED", "Knowledge base is archived");
        }
        if (!kb.getVersion().equals(request.version())) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Knowledge base was updated concurrently");
        }

        int chunkSize = request.chunkSize() != null ? request.chunkSize() : kb.getChunkSize();
        int chunkOverlap = request.chunkOverlap() != null ? request.chunkOverlap() : kb.getChunkOverlap();
        validateChunkConfig(chunkSize, chunkOverlap);
        int topK = request.defaultTopK() != null ? request.defaultTopK() : kb.getDefaultTopK();
        if (topK < 1 || topK > properties.getMaximumTopK()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "defaultTopK out of range");
        }

        kb.setName(request.name().trim());
        kb.setDescription(request.description());
        kb.setChunkSize(chunkSize);
        kb.setChunkOverlap(chunkOverlap);
        kb.setDefaultTopK(topK);
        if (request.minimumScore() != null) {
            kb.setMinimumScore(request.minimumScore());
        }
        kb.setUpdatedBy(user.getUserId());
        kb.setUpdatedAt(Instant.now());
        try {
            return mapper.toBaseResponse(knowledgeBaseRepository.save(kb));
        } catch (ObjectOptimisticLockingFailureException ex) {
            throw new ApiException(HttpStatus.CONFLICT, "OPTIMISTIC_LOCK", "Knowledge base was updated concurrently");
        }
    }

    @Transactional
    public KnowledgeBaseResponse activate(UUID projectId, UUID id, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_ACTIVATE);
        KnowledgeBase kb = requireKnowledgeBase(projectId, id, user.getOrganizationId());
        if (kb.getStatus() == KnowledgeBaseStatus.ARCHIVED) {
            throw new ApiException(HttpStatus.CONFLICT, "KNOWLEDGE_ARCHIVED", "Knowledge base is archived");
        }
        if (kb.getStatus() == KnowledgeBaseStatus.ACTIVE) {
            return mapper.toBaseResponse(kb);
        }
        requireProvider(kb.getEmbeddingProviderKey());
        validateChunkConfig(kb.getChunkSize(), kb.getChunkOverlap());
        kb.setStatus(KnowledgeBaseStatus.ACTIVE);
        kb.setUpdatedBy(user.getUserId());
        kb.setUpdatedAt(Instant.now());
        return mapper.toBaseResponse(knowledgeBaseRepository.save(kb));
    }

    @Transactional
    public void archive(UUID projectId, UUID id, AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_ARCHIVE);
        KnowledgeBase kb = requireKnowledgeBase(projectId, id, user.getOrganizationId());
        if (kb.getStatus() == KnowledgeBaseStatus.ARCHIVED) {
            return;
        }
        kb.setStatus(KnowledgeBaseStatus.ARCHIVED);
        kb.setUpdatedBy(user.getUserId());
        kb.setUpdatedAt(Instant.now());
        knowledgeBaseRepository.save(kb);

        List<AgentKnowledgeAssignment> assignments =
                assignmentRepository.findByKnowledgeBaseIdAndProjectIdAndOrganizationId(
                        id, projectId, user.getOrganizationId());
        Instant now = Instant.now();
        for (AgentKnowledgeAssignment assignment : assignments) {
            if (assignment.isEnabled()) {
                assignment.setEnabled(false);
                assignment.setUpdatedBy(user.getUserId());
                assignment.setUpdatedAt(now);
            }
        }
        assignmentRepository.saveAll(assignments);
    }

    private EmbeddingProvider requireProvider(String key) {
        if (!embeddingProviderRegistry.isRegistered(key)) {
            throw new ApiException(
                    HttpStatus.BAD_REQUEST, "EMBEDDING_PROVIDER_UNSUPPORTED", "Embedding provider is not allowlisted");
        }
        return embeddingProviderRegistry.require(key);
    }

    private void validateChunkConfig(int chunkSize, int chunkOverlap) {
        if (chunkSize < 100 || chunkSize > 5000) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "chunkSize out of range");
        }
        if (chunkOverlap < 0 || chunkOverlap >= chunkSize) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "chunkOverlap out of range");
        }
    }

    public KnowledgeBase requireKnowledgeBase(UUID projectId, UUID id, UUID organizationId) {
        return knowledgeBaseRepository
                .findByIdAndProjectIdAndOrganizationId(id, projectId, organizationId)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, "KNOWLEDGE_BASE_NOT_FOUND", "Knowledge base not found"));
    }

    private void requireProject(UUID projectId, UUID organizationId) {
        projectRepository
                .findByIdAndOrganizationId(projectId, organizationId)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
    }
}
