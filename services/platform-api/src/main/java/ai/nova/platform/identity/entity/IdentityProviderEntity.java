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
@Table(name = "identity_providers")
public class IdentityProviderEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private ProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ProviderStatus status;

    @Column(name = "config_json")
    private String configJson;

    @Column(name = "is_default", nullable = false)
    private boolean defaultProvider;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected IdentityProviderEntity() {
    }

    public IdentityProviderEntity(
            UUID id,
            UUID organizationId,
            String name,
            ProviderType providerType,
            ProviderStatus status,
            String configJson,
            boolean defaultProvider,
            Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.name = name;
        this.providerType = providerType;
        this.status = status;
        this.configJson = configJson;
        this.defaultProvider = defaultProvider;
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

    public ProviderType getProviderType() {
        return providerType;
    }

    public ProviderStatus getStatus() {
        return status;
    }

    public String getConfigJson() {
        return configJson;
    }

    public boolean isDefaultProvider() {
        return defaultProvider;
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

    public void setStatus(ProviderStatus status) {
        this.status = status;
    }

    public void setConfigJson(String configJson) {
        this.configJson = configJson;
    }

    public void setDefaultProvider(boolean defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public void touch(Instant now) {
        this.updatedAt = now;
    }
}
