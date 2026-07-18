package ai.nova.platform.knowledge.audit;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.knowledge.entity.KnowledgeRetrievalAudit;
import ai.nova.platform.knowledge.repository.KnowledgeRetrievalAuditRepository;
import ai.nova.platform.web.correlation.CorrelationIdFilter;

@Service
public class KnowledgeRetrievalAuditService {

    private static final Logger log = LoggerFactory.getLogger(KnowledgeRetrievalAuditService.class);

    private final KnowledgeRetrievalAuditRepository auditRepository;

    public KnowledgeRetrievalAuditService(KnowledgeRetrievalAuditRepository auditRepository) {
        this.auditRepository = auditRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void record(
            UUID organizationId,
            UUID projectId,
            UUID knowledgeBaseId,
            UUID agentId,
            UUID executionId,
            UUID conversationId,
            String queryHash,
            int queryCharacterCount,
            int requestedTopK,
            int candidateCount,
            int returnedCount,
            BigDecimal minimumScore,
            long durationMs,
            UUID performedBy) {
        try {
            auditRepository.saveAndFlush(new KnowledgeRetrievalAudit(
                    UUID.randomUUID(),
                    organizationId,
                    projectId,
                    knowledgeBaseId,
                    agentId,
                    executionId,
                    conversationId,
                    queryHash,
                    queryCharacterCount,
                    requestedTopK,
                    candidateCount,
                    returnedCount,
                    minimumScore,
                    durationMs,
                    performedBy,
                    Instant.now(),
                    MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)));
        } catch (RuntimeException ex) {
            log.debug("Knowledge retrieval audit failed for knowledgeBase {}", knowledgeBaseId);
        }
    }
}
