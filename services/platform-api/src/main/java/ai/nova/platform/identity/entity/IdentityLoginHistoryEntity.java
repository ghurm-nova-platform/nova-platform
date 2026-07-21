package ai.nova.platform.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "identity_login_history")
public class IdentityLoginHistoryEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "identity_user_id")
    private UUID identityUserId;

    @Column(name = "provider_id")
    private UUID providerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LoginResult result;

    @Column(name = "ip_address", length = 64)
    private String ipAddress;

    @Column(name = "user_agent", length = 500)
    private String userAgent;

    @Column(name = "failure_reason", length = 500)
    private String failureReason;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentityLoginHistoryEntity() {
    }

    public IdentityLoginHistoryEntity(
            UUID id,
            UUID organizationId,
            UUID identityUserId,
            UUID providerId,
            LoginResult result,
            String ipAddress,
            String userAgent,
            String failureReason,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.identityUserId = identityUserId;
        this.providerId = providerId;
        this.result = result;
        this.ipAddress = ipAddress;
        this.userAgent = userAgent;
        this.failureReason = failureReason;
        this.createdAt = createdAt;
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

    public UUID getProviderId() {
        return providerId;
    }

    public LoginResult getResult() {
        return result;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
