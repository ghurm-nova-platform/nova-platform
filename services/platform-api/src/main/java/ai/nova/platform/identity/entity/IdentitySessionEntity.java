package ai.nova.platform.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "identity_sessions")
public class IdentitySessionEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "identity_user_id", nullable = false)
    private UUID identityUserId;

    @Column(name = "platform_user_id")
    private UUID platformUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SessionStatus status;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "last_accessed_at", nullable = false)
    private Instant lastAccessedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Version
    @Column(nullable = false)
    private int version;

    protected IdentitySessionEntity() {
    }

    public IdentitySessionEntity(
            UUID id,
            UUID organizationId,
            UUID identityUserId,
            UUID platformUserId,
            String ipAddress,
            String userAgent,
            Instant createdAt,
            Instant expiresAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.identityUserId = identityUserId;
        this.platformUserId = platformUserId;
        this.status = SessionStatus.ACTIVE;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.createdAt = createdAt;
        this.lastAccessedAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getIdentityUserId() {
        return identityUserId;
    }

    public UUID getPlatformUserId() {
        return platformUserId;
    }

    public SessionStatus getStatus() {
        return status;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getLastAccessedAt() {
        return lastAccessedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public int getVersion() {
        return version;
    }

    public boolean isActive(Instant now) {
        return status == SessionStatus.ACTIVE && revokedAt == null && expiresAt.isAfter(now);
    }

    public void revoke(Instant at) {
        this.status = SessionStatus.REVOKED;
        this.revokedAt = at;
    }

    public void expire(Instant at) {
        this.status = SessionStatus.EXPIRED;
        this.revokedAt = at;
    }

    public void touch(Instant now) {
        this.lastAccessedAt = now;
    }
}
