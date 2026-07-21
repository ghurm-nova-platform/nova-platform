package ai.nova.platform.audit.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_correlation")
public class AuditCorrelationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "correlation_id", length = 64)
    private String correlationId;

    @Column(name = "request_id", length = 64)
    private String requestId;

    @Column(name = "session_id")
    private UUID sessionId;

    @Column(name = "audit_event_id", nullable = false)
    private UUID auditEventId;

    @Column(name = "chain_sequence", nullable = false)
    private int chainSequence;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AuditCorrelationEntity() {
    }

    public AuditCorrelationEntity(
            UUID id,
            UUID organizationId,
            String correlationId,
            String requestId,
            UUID sessionId,
            UUID auditEventId,
            int chainSequence,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.correlationId = correlationId;
        this.requestId = requestId;
        this.sessionId = sessionId;
        this.auditEventId = auditEventId;
        this.chainSequence = chainSequence;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public String getRequestId() {
        return requestId;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getAuditEventId() {
        return auditEventId;
    }

    public int getChainSequence() {
        return chainSequence;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
