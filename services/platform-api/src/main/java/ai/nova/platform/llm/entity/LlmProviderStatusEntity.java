package ai.nova.platform.llm.entity;

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
@Table(name = "llm_provider_status")
public class LlmProviderStatusEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private LlmProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LlmProviderHealthStatus status;

    @Column(name = "endpoint_url", length = 1000)
    private String endpointUrl;

    @Column(name = "last_health_check_at")
    private Instant lastHealthCheckAt;

    @Column(name = "last_error")
    private String lastError;

    @Column(name = "metadata_json", nullable = false)
    private String metadataJson;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LlmProviderStatusEntity() {
    }

    public LlmProviderStatusEntity(
            UUID id,
            UUID organizationId,
            LlmProviderType providerType,
            LlmProviderHealthStatus status,
            Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.providerType = providerType;
        this.status = status;
        this.metadataJson = "{}";
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public LlmProviderType getProviderType() { return providerType; }
    public LlmProviderHealthStatus getStatus() { return status; }
    public String getEndpointUrl() { return endpointUrl; }
    public Instant getLastHealthCheckAt() { return lastHealthCheckAt; }
    public String getLastError() { return lastError; }
    public String getMetadataJson() { return metadataJson; }
    public int getVersion() { return version; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setStatus(LlmProviderHealthStatus status) { this.status = status; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public void setLastHealthCheckAt(Instant lastHealthCheckAt) { this.lastHealthCheckAt = lastHealthCheckAt; }
    public void setLastError(String lastError) { this.lastError = lastError; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public void touch(Instant now) { this.updatedAt = now; }
}
