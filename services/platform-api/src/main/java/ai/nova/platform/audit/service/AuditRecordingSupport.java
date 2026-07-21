package ai.nova.platform.audit.service;

import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ai.nova.platform.audit.dto.AuditDtos.RecordAuditEventRequest;
import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;
import ai.nova.platform.security.AuthenticatedUser;

@Component
public class AuditRecordingSupport {

    private final AuditPublisher auditPublisher;

    public AuditRecordingSupport(AuditPublisher auditPublisher) {
        this.auditPublisher = auditPublisher;
    }

    public void recordDomainEvent(
            AuthenticatedUser user,
            UUID projectId,
            AuditEntityType entityType,
            UUID entityId,
            String entityLabel,
            AuditAction action,
            AuditResult result,
            AuditSource source,
            Map<String, Object> details) {
        if (user == null) {
            return;
        }
        auditPublisher.record(new RecordAuditEventRequest(
                user.getOrganizationId(),
                projectId,
                user.getUserId(),
                user.getDisplayName(),
                null,
                entityType,
                entityId,
                entityLabel,
                action,
                result,
                AuditSeverity.MEDIUM,
                source,
                null,
                null,
                null,
                null,
                details));
    }

    public void recordSecurityEvent(
            UUID organizationId,
            UUID userId,
            String username,
            UUID sessionId,
            AuditAction action,
            AuditResult result,
            String ipAddress,
            String userAgent) {
        auditPublisher.record(new RecordAuditEventRequest(
                organizationId,
                null,
                userId,
                username,
                sessionId,
                AuditEntityType.USER,
                userId,
                username,
                action,
                result,
                AuditSeverity.MEDIUM,
                AuditSource.PORTAL,
                null,
                null,
                ipAddress,
                userAgent,
                Map.of()));
    }
}
