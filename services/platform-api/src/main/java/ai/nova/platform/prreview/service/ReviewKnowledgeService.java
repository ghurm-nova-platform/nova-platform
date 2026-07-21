package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ai.nova.platform.knowledge.engine.dto.KnowledgeEngineDtos.MemoryDocument;
import ai.nova.platform.knowledge.engine.entity.KnowledgeType;
import ai.nova.platform.knowledge.engine.security.KnowledgeEngineAuthorizationService;
import ai.nova.platform.knowledge.engine.service.KnowledgeMemoryService;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class ReviewKnowledgeService {

    private static final List<KnowledgeType> MEMORY_TYPES = List.of(
            KnowledgeType.ADR,
            KnowledgeType.DECISION,
            KnowledgeType.BUG,
            KnowledgeType.FIX,
            KnowledgeType.PULL_REQUEST,
            KnowledgeType.RELEASE,
            KnowledgeType.BEST_PRACTICE,
            KnowledgeType.RUNBOOK);

    private final KnowledgeMemoryService knowledgeMemoryService;

    public ReviewKnowledgeService(KnowledgeMemoryService knowledgeMemoryService) {
        this.knowledgeMemoryService = knowledgeMemoryService;
    }

    public List<FindingDraft> attachKnowledge(
            List<FindingDraft> findings, AuthenticatedUser user, UUID projectId) {
        if (findings == null || findings.isEmpty()) {
            return findings == null ? List.of() : findings;
        }
        if (!canReadKnowledge(user)) {
            return findings;
        }
        List<MemoryDocument> documents =
                knowledgeMemoryService.getRelevantDocuments(user, projectId, MEMORY_TYPES, 50);
        if (documents == null || documents.isEmpty()) {
            return findings;
        }
        List<FindingDraft> enriched = new ArrayList<>(findings.size());
        for (FindingDraft finding : findings) {
            List<UUID> matched = matchDocuments(finding, documents);
            enriched.add(matched.isEmpty() ? finding : finding.withKnowledgeDocumentIds(matched));
        }
        return enriched;
    }

    private boolean canReadKnowledge(AuthenticatedUser user) {
        if (user == null) {
            return false;
        }
        if (user.getRoles().contains("ORG_ADMIN")) {
            return true;
        }
        return user.hasPermission(KnowledgeEngineAuthorizationService.KNOWLEDGE_READ);
    }

    private List<UUID> matchDocuments(FindingDraft finding, List<MemoryDocument> documents) {
        Set<String> tokens = tokens(finding.title() + " " + finding.description());
        if (tokens.isEmpty()) {
            return List.of();
        }
        List<UUID> matched = new ArrayList<>();
        for (MemoryDocument document : documents) {
            Set<String> docTokens = tokens((document.title() == null ? "" : document.title()) + " "
                    + (document.summary() == null ? "" : document.summary()));
            long overlap = tokens.stream().filter(docTokens::contains).count();
            if (overlap >= 1) {
                matched.add(document.id());
            }
            if (matched.size() >= 5) {
                break;
            }
        }
        return matched;
    }

    private Set<String> tokens(String text) {
        if (text == null || text.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("[^a-z0-9]+"))
                .filter(token -> token.length() >= 4)
                .filter(token -> !STOP_WORDS.contains(token))
                .collect(Collectors.toCollection(HashSet::new));
    }

    private static final Set<String> STOP_WORDS = Set.of(
            "this",
            "that",
            "with",
            "from",
            "have",
            "should",
            "which",
            "using",
            "into",
            "diff",
            "code",
            "appears",
            "contains");
}
