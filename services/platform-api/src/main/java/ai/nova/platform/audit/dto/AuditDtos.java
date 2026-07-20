package ai.nova.platform.audit.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ai.nova.platform.audit.entity.AuditAction;
import ai.nova.platform.audit.entity.AuditEntityType;
import ai.nova.platform.audit.entity.AuditResult;
import ai.nova.platform.audit.entity.AuditSeverity;
import ai.nova.platform.audit.entity.AuditSource;

public final class AuditDtos {

    private AuditDtos() {
    }

    public record AuditEvent(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID userId,
            String username,
            UUID sessionId,
            AuditEntityType entityType,
            UUID entityId,
            String entityLabel,
            AuditAction action,
            AuditResult result,
            AuditSeverity severity,
            AuditSource source,
            String correlationId,
            String requestId,
            String ipAddress,
            String userAgent,
            Map<String, Object> details,
            Instant createdAt) {
    }

    public record RecordAuditEventRequest(
            UUID organizationId,
            UUID projectId,
            UUID userId,
            String username,
            UUID sessionId,
            AuditEntityType entityType,
            UUID entityId,
            String entityLabel,
            AuditAction action,
            AuditResult result,
            AuditSeverity severity,
            AuditSource source,
            String correlationId,
            String requestId,
            String ipAddress,
            String userAgent,
            Map<String, Object> details) {
    }

    public record AuditSearchRequest(
            Instant from,
            Instant to,
            UUID projectId,
            UUID userId,
            AuditEntityType entityType,
            UUID entityId,
            AuditAction action,
            AuditSeverity severity,
            AuditResult result,
            String correlationId,
            String requestId,
            int page,
            int size) {
    }

    public record AuditSearchResponse(List<AuditEvent> events, long total, int page, int size) {
    }

    public record AuditHistoryResponse(
            AuditEntityType entityType, UUID entityId, String entityLabel, List<AuditEvent> events) {
    }

    public record AuditCorrelationView(
            String correlationId,
            String requestId,
            UUID sessionId,
            List<AuditEvent> events) {
    }
}
