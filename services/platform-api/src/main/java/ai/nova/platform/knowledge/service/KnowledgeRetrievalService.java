package ai.nova.platform.knowledge.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.runtime.RuntimeKnowledgeChunk;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeCitation;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeContext;
import ai.nova.platform.knowledge.audit.KnowledgeRetrievalAuditService;
import ai.nova.platform.knowledge.chunking.ParagraphAwareTextChunker;
import ai.nova.platform.knowledge.config.KnowledgeProperties;
import ai.nova.platform.knowledge.embedding.EmbeddingProvider;
import ai.nova.platform.knowledge.embedding.EmbeddingProviderRegistry;
import ai.nova.platform.knowledge.entity.AgentKnowledgeAssignment;
import ai.nova.platform.knowledge.entity.KnowledgeBase;
import ai.nova.platform.knowledge.entity.KnowledgeBaseStatus;
import ai.nova.platform.knowledge.entity.KnowledgeChunk;
import ai.nova.platform.knowledge.entity.KnowledgeDocument;
import ai.nova.platform.knowledge.entity.KnowledgeDocumentStatus;
import ai.nova.platform.knowledge.repository.AgentKnowledgeAssignmentRepository;
import ai.nova.platform.knowledge.repository.KnowledgeBaseRepository;
import ai.nova.platform.knowledge.repository.KnowledgeChunkRepository;
import ai.nova.platform.knowledge.repository.KnowledgeDocumentRepository;
import ai.nova.platform.knowledge.security.KnowledgeAuthorizationService;
import ai.nova.platform.knowledge.vector.KnowledgeVectorStore;
import ai.nova.platform.knowledge.vector.VectorSearchHit;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class KnowledgeRetrievalService {

    private final KnowledgeProperties properties;
    private final AgentKnowledgeAssignmentRepository assignmentRepository;
    private final KnowledgeBaseRepository knowledgeBaseRepository;
    private final KnowledgeDocumentRepository documentRepository;
    private final KnowledgeChunkRepository chunkRepository;
    private final EmbeddingProviderRegistry embeddingProviderRegistry;
    private final KnowledgeVectorStore vectorStore;
    private final KnowledgeRetrievalAuditService auditService;
    private final KnowledgeAuthorizationService authorizationService;

    public KnowledgeRetrievalService(
            KnowledgeProperties properties,
            AgentKnowledgeAssignmentRepository assignmentRepository,
            KnowledgeBaseRepository knowledgeBaseRepository,
            KnowledgeDocumentRepository documentRepository,
            KnowledgeChunkRepository chunkRepository,
            EmbeddingProviderRegistry embeddingProviderRegistry,
            KnowledgeVectorStore vectorStore,
            KnowledgeRetrievalAuditService auditService,
            KnowledgeAuthorizationService authorizationService) {
        this.properties = properties;
        this.assignmentRepository = assignmentRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.embeddingProviderRegistry = embeddingProviderRegistry;
        this.vectorStore = vectorStore;
        this.auditService = auditService;
        this.authorizationService = authorizationService;
    }

    public record RetrievalResult(RuntimeKnowledgeContext context, int candidateCount) {
        public static RetrievalResult empty() {
            return new RetrievalResult(RuntimeKnowledgeContext.empty(), 0);
        }
    }

    @Transactional(readOnly = true)
    public RetrievalResult retrieve(
            UUID projectId,
            UUID agentId,
            String query,
            UUID executionId,
            UUID conversationId,
            AuthenticatedUser user) {
        authorizationService.require(user, KnowledgeAuthorizationService.KNOWLEDGE_RETRIEVE);
        if (!properties.isRetrievalEnabled()) {
            return RetrievalResult.empty();
        }
        if (query == null || query.isBlank()) {
            return RetrievalResult.empty();
        }
        String normalizedQuery = query.trim();
        if (normalizedQuery.length() > properties.getMaxQueryCharacters()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "QUERY_TOO_LONG", "Query exceeds maximum characters");
        }

        List<AgentKnowledgeAssignment> assignments =
                assignmentRepository.findByAgentIdAndProjectIdAndOrganizationIdAndEnabledTrueOrderByCreatedAtAsc(
                        agentId, projectId, user.getOrganizationId());
        if (assignments.isEmpty()) {
            return RetrievalResult.empty();
        }

        List<UUID> kbIds = assignments.stream().map(AgentKnowledgeAssignment::getKnowledgeBaseId).toList();
        Map<UUID, KnowledgeBase> bases = knowledgeBaseRepository
                .findByIdInAndProjectIdAndOrganizationId(kbIds, projectId, user.getOrganizationId())
                .stream()
                .filter(kb -> kb.getStatus() == KnowledgeBaseStatus.ACTIVE)
                .collect(java.util.stream.Collectors.toMap(KnowledgeBase::getId, kb -> kb, (a, b) -> a, LinkedHashMap::new));

        List<RuntimeKnowledgeCitation> citations = new ArrayList<>();
        List<RuntimeKnowledgeChunk> chunks = new ArrayList<>();
        Set<String> seenHashes = new HashSet<>();
        int totalCandidates = 0;
        int charBudget = properties.getMaxRetrievedCharacters();
        int citationIndex = 1;
        String queryHash = ParagraphAwareTextChunker.sha256(normalizedQuery);

        for (AgentKnowledgeAssignment assignment : assignments) {
            KnowledgeBase kb = bases.get(assignment.getKnowledgeBaseId());
            if (kb == null) {
                continue;
            }
            // Recheck assignment still enabled
            if (!assignment.isEnabled()) {
                continue;
            }

            int topK = assignment.getTopKOverride() != null ? assignment.getTopKOverride() : kb.getDefaultTopK();
            topK = Math.min(Math.max(topK, 1), properties.getMaximumTopK());
            double minimumScore = assignment.getMinimumScoreOverride() != null
                    ? assignment.getMinimumScoreOverride().doubleValue()
                    : (kb.getMinimumScore() != null
                            ? kb.getMinimumScore().doubleValue()
                            : properties.getDefaultMinimumScore());

            EmbeddingProvider provider = embeddingProviderRegistry.require(kb.getEmbeddingProviderKey());
            float[] queryEmbedding = provider.embed(normalizedQuery);

            long started = System.nanoTime();
            List<VectorSearchHit> hits = vectorStore.search(
                    user.getOrganizationId(),
                    projectId,
                    kb.getId(),
                    provider.providerKey(),
                    provider.model(),
                    queryEmbedding,
                    topK,
                    minimumScore,
                    properties.getMaxVectorCandidates());
            long durationMs = (System.nanoTime() - started) / 1_000_000L;
            totalCandidates += hits.size();

            List<UUID> hitChunkIds = hits.stream().map(VectorSearchHit::chunkId).toList();
            Map<UUID, KnowledgeChunk> chunkMap = chunkRepository
                    .findByIdInAndProjectIdAndOrganizationId(hitChunkIds, projectId, user.getOrganizationId())
                    .stream()
                    .collect(java.util.stream.Collectors.toMap(KnowledgeChunk::getId, c -> c));
            List<UUID> docIds = chunkMap.values().stream().map(KnowledgeChunk::getDocumentId).distinct().toList();
            Map<UUID, KnowledgeDocument> documents = documentRepository
                    .findByIdInAndProjectIdAndOrganizationId(docIds, projectId, user.getOrganizationId())
                    .stream()
                    .filter(doc -> doc.getStatus() == KnowledgeDocumentStatus.READY)
                    .collect(java.util.stream.Collectors.toMap(KnowledgeDocument::getId, d -> d));

            int returnedForKb = 0;
            for (VectorSearchHit hit : hits) {
                KnowledgeChunk chunk = chunkMap.get(hit.chunkId());
                if (chunk == null) {
                    continue;
                }
                KnowledgeDocument document = documents.get(chunk.getDocumentId());
                if (document == null) {
                    continue;
                }
                if (!seenHashes.add(chunk.getContentHash())) {
                    continue;
                }
                String content = chunk.getContent();
                if (content.length() > charBudget) {
                    if (charBudget <= 0) {
                        break;
                    }
                    content = content.substring(0, charBudget);
                }
                charBudget -= content.length();
                String label = "K" + citationIndex++;
                citations.add(new RuntimeKnowledgeCitation(
                        label,
                        kb.getId(),
                        kb.getName(),
                        document.getId(),
                        document.getFileName(),
                        chunk.getChunkIndex(),
                        hit.score()));
                chunks.add(new RuntimeKnowledgeChunk(
                        label,
                        chunk.getId(),
                        kb.getId(),
                        document.getId(),
                        chunk.getChunkIndex(),
                        content,
                        hit.score()));
                returnedForKb++;
                if (charBudget <= 0) {
                    break;
                }
            }

            BigDecimal minScoreBd = assignment.getMinimumScoreOverride() != null
                    ? assignment.getMinimumScoreOverride()
                    : kb.getMinimumScore();
            try {
                auditService.record(
                        user.getOrganizationId(),
                        projectId,
                        kb.getId(),
                        agentId,
                        executionId,
                        conversationId,
                        queryHash,
                        normalizedQuery.length(),
                        topK,
                        hits.size(),
                        returnedForKb,
                        minScoreBd,
                        durationMs,
                        user.getUserId());
            } catch (RuntimeException ignored) {
                // best effort
            }

            if (charBudget <= 0) {
                break;
            }
        }

        return new RetrievalResult(new RuntimeKnowledgeContext(citations, chunks), totalCandidates);
    }

    public boolean hasEnabledAssignments(UUID projectId, UUID agentId, UUID organizationId) {
        return assignmentRepository.existsByAgentIdAndProjectIdAndOrganizationIdAndEnabledTrue(
                agentId, projectId, organizationId);
    }
}
