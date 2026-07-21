package ai.nova.platform.knowledge.engine.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.MemoryDocument;
import ai.nova.platform.knowledge.engine.entity.DocumentStatus;
import ai.nova.platform.knowledge.engine.entity.KnowledgeDocumentEntity;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.repository.KnowledgeEngineDocumentRepository;
import ai.nova.platform.knowledge.engine.security.KnowledgeEngineAuthorizationService;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class KnowledgeMemoryService {

    private static final List<KnowledgeType> DEFAULT_MEMORY_TYPES = List.of(
            KnowledgeType.ADR,
            KnowledgeType.PULL_REQUEST,
            KnowledgeType.BUG,
            KnowledgeType.FIX,
            KnowledgeType.DEPLOYMENT,
            KnowledgeType.DECISION,
            KnowledgeType.RUNBOOK);

    private final KnowledgeEngineAuthorizationService authorizationService;
    private final KnowledgeVisibilityService visibilityService;
    private final KnowledgeEngineDocumentRepository documentRepository;

    public KnowledgeMemoryService(
            KnowledgeEngineAuthorizationService authorizationService,
            KnowledgeVisibilityService visibilityService,
            KnowledgeEngineDocumentRepository documentRepository) {
        this.authorizationService = authorizationService;
        this.visibilityService = visibilityService;
        this.documentRepository = documentRepository;
    }

    @Transactional(readOnly = true)
    public List<MemoryDocument> getRelevantDocuments(
            AuthenticatedUser user, java.util.UUID projectId, List<KnowledgeType> knowledgeTypes, int limit) {
        authorizationService.requireRead(user);
        List<KnowledgeType> types =
                knowledgeTypes == null || knowledgeTypes.isEmpty() ? DEFAULT_MEMORY_TYPES : knowledgeTypes;
        int effectiveLimit = limit <= 0 ? 20 : Math.min(limit, 100);
        return documentRepository
                .findByOrganizationIdAndKnowledgeTypeIn(
                        user.getOrganizationId(), DocumentStatus.ACTIVE, types, projectId)
                .stream()
                .filter(doc -> visibilityService.canRead(user, doc))
                .limit(effectiveLimit)
                .map(this::toMemoryDocument)
                .collect(Collectors.toList());
    }

    private MemoryDocument toMemoryDocument(KnowledgeDocumentEntity doc) {
        return new MemoryDocument(
                doc.getId(),
                doc.getTitle(),
                doc.getSummary(),
                doc.getKnowledgeType(),
                doc.getCategory(),
                doc.getProjectId(),
                doc.getUpdatedAt());
    }
}
