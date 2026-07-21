package ai.nova.platform.audit.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_events")
public class AuditEventEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "username", length = 200)
    private String username;

    @Column(name = "session_id")
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 40)
    private AuditEntityType entityType;

    @Column(name = "entity_id")
    private UUID entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 40)
    private AuditAction action;

    @Enumerated(EnumType.STRING)
    @Column(name = "result", nullable = false, length = 20)
    private AuditResult result;

    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private AuditSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false, length = 60)
    private AuditSource source;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "details_json")
    private String detailsJson;

    @Column(name = "event_fingerprint", nullable = false, length = 64)
    private String eventFingerprint;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditEventEntity() {
    }

    public AuditEventEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID userId,
            String username,
            UUID sessionId,
            AuditEntityType entityType,
            UUID entityId,
            AuditAction action,
            AuditResult result,
            AuditSeverity severity,
            AuditSource source,
            String correlationId,
            String requestId,
            String ipAddress,
            String userAgent,
            String detailsJson,
            String eventFingerprint,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.userId = userId;
        this.username = username;
        this.sessionId = sessionId;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.result = result;
        this.severity = severity;
        this.source = source;
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.detailsJson = detailsJson;
        this.eventFingerprint = eventFingerprint;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public AuditEntityType getEntityType() {
        return entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public AuditAction getAction() {
        return action;
    }

    public AuditResult getResult() {
        return result;
    }

    public AuditSeverity getSeverity() {
        return severity;
    }

    public AuditSource getSource() {
        return source;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public String getEventFingerprint() {
        return eventFingerprint;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
