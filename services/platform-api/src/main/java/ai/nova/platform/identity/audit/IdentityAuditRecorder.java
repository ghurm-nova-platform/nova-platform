package ai.nova.platform.identity.audit;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.audit.service.AuditRecordingSupport;
import ai.nova.platform.security.AuthenticatedUser;

@Component
public class IdentityAuditRecorder {

    private final AuditRecordingSupport auditRecordingSupport;

    public IdentityAuditRecorder(AuditRecordingSupport auditRecordingSupport) {
        this.auditRecordingSupport = auditRecordingSupport;
    }

    public void recordIdentityEvent(
            AuthenticatedUser user,
            UUID entityId,
            AuditAction action,
            AuditResult result,
            Map<String, Object> details) {
        auditRecordingSupport.recordDomainEvent(
                user,
                null,
                AuditEntityType.IDENTITY,
                entityId,
                "identity",
                action,
                result,
                AuditSource.IDENTITY,
                details);
    }
}
