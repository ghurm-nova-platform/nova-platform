package ai.nova.platform.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "identity_api_tokens")
public class IdentityApiTokenEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "identity_user_id")
    private UUID identityUserId;

    @Column(name = "service_account_id")
    private UUID serviceAccountId;

    @Column(nullable = false)
    private String name;

    @Column(name = "token_prefix", nullable = false, length = 16)
    private String tokenPrefix;

    @Column(name = "token_hash", nullable = false, unique = true, length = 128)
    private String tokenHash;

    @Column(name = "scopes_json")
    private String scopesJson;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @Column(name = "revoked_at")
    private Instant revokedAt;

    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected IdentityApiTokenEntity() {
    }

    public IdentityApiTokenEntity(
            UUID id,
            UUID organizationId,
            UUID identityUserId,
            String name,
            String tokenPrefix,
            String tokenHash,
            String scopesJson,
            Instant expiresAt,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.identityUserId = identityUserId;
        this.name = name;
        this.tokenPrefix = tokenPrefix;
        this.tokenHash = tokenHash;
        this.scopesJson = scopesJson;
        this.expiresAt = expiresAt;
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

    public UUID getServiceAccountId() {
        return serviceAccountId;
    }

    public String getName() {
        return name;
    }

    public String getTokenPrefix() {
        return tokenPrefix;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getScopesJson() {
        return scopesJson;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public boolean isActive(Instant now) {
        if (revokedAt != null) {
            return false;
        }
        return expiresAt == null || expiresAt.isAfter(now);
    }

    public void revoke(Instant at) {
        this.revokedAt = at;
    }

    public void markUsed(Instant at) {
        this.lastUsedAt = at;
    }
}
