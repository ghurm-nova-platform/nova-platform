package ai.nova.platform.audit.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "audit_sessions")
public class AuditSessionEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false, unique = true)
    private UUID sessionId;

    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "ended_at")
    private Instant endedAt;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    protected AuditSessionEntity() {
    }

    public AuditSessionEntity(
            UUID id,
            UUID sessionId,
            UUID userId,
            UUID organizationId,
            Instant startedAt,
            Instant endedAt,
            String ipAddress,
            String userAgent) {
        this.id = id;
        this.sessionId = sessionId;
        this.userId = userId;
        this.organizationId = organizationId;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setEndedAt(Instant endedAt) {
        this.endedAt = endedAt;
    }
}
