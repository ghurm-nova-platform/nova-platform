package ai.nova.platform.identity.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "identity_service_accounts")
public class IdentityServiceAccountEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Column(name = "client_id", nullable = false, unique = true)
    private String clientId;

    @Column(name = "client_secret_hash", nullable = false, length = 128)
    private String clientSecretHash;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityServiceAccountEntity() {
    }

    public IdentityServiceAccountEntity(
            UUID id,
            UUID organizationId,
            String name,
            String clientId,
            String clientSecretHash,
            Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.clientId = clientId;
        this.clientSecretHash = clientSecretHash;
        this.enabled = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public String getName() {
        return name;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecretHash() {
        return clientSecretHash;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public int getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
