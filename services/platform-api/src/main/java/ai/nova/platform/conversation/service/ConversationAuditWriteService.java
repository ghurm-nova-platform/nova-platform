package ai.nova.platform.conversation.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.conversation.entity.ConversationAuditAction;
import ai.nova.platform.conversation.entity.ConversationAuditLog;
import ai.nova.platform.conversation.repository.ConversationAuditLogRepository;
import ai.nova.platform.web.correlation.CorrelationIdFilter;

/**
 * Write-side audit persistence for conversation memory events.
 * Uses an independent write transaction so read-only assembly can stay read-only.
 */
@Service
public class ConversationAuditWriteService {

    private final ConversationAuditLogRepository auditLogRepository;

    public ConversationAuditWriteService(ConversationAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void writeMemoryTruncated(
            UUID conversationId,
            UUID organizationId,
            UUID projectId,
            int droppedCount,
            int totalPriorMessages,
            int includedPriorMessages,
            UUID performedBy) {
        String metadata = String.format(
                "{\"droppedCount\":%d,\"totalPriorMessages\":%d,\"includedPriorMessages\":%d}",
                droppedCount, totalPriorMessages, includedPriorMessages);
        auditLogRepository.save(new ConversationAuditLog(
                UUID.randomUUID(),
                conversationId,
                organizationId,
                projectId,
                ConversationAuditAction.MEMORY_TRUNCATED,
                metadata,
                performedBy,
                Instant.now(),
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)));
    }
}
