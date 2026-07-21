package ai.nova.platform.llm.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class LlmAuditService {

    private final AuditRecordingSupport auditRecordingSupport;

    public LlmAuditService(AuditRecordingSupport auditRecordingSupport) {
        this.auditRecordingSupport = auditRecordingSupport;
    }

    public void record(
            AuthenticatedUser user,
            UUID entityId,
            String entityLabel,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        auditRecordingSupport.recordDomainEvent(
                user,
                null,
                AuditEntityType.LLM_RUNTIME,
                entityId,
                entityLabel,
                action,
                result,
                AuditSource.LLM_RUNTIME,
                details == null ? Map.of() : details);
    }
}
