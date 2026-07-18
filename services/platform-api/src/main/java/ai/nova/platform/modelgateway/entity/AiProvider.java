package ai.nova.platform.modelgateway.entity;

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
@Table(name = "ai_providers")
public class AiProvider {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "provider_key", nullable = false, length = 100)
    private String providerKey;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 50)
    private AiProviderType providerType;

    @Column(name = "adapter_key", nullable = false, length = 100)
    private String adapterKey;

    @Column(name = "credential_reference", length = 500)
    private String credentialReference;

    @Column(length = 100)
    private String region;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiProviderStatus status;

    @Column(name = "request_timeout_seconds", nullable = false)
    private Integer requestTimeoutSeconds;

    @Column(name = "max_concurrent_requests", nullable = false)
    private Integer maxConcurrentRequests;

    @Column(name = "max_retries", nullable = false)
    private Integer maxRetries;

    @Column(name = "retry_backoff_ms", nullable = false)
    private Integer retryBackoffMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "endpoint_profile", length = 50)
    private EndpointProfile endpointProfile;

    @Column(name = "azure_resource_name", length = 100)
    private String azureResourceName;

    @Column(name = "azure_api_version", length = 50)
    private String azureApiVersion;

    @Column(name = "last_connection_test_at")
    private Instant lastConnectionTestAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_connection_test_status", length = 30)
    private ConnectionTestStatus lastConnectionTestStatus;

    @Column(name = "last_connection_test_error_code", length = 100)
    private String lastConnectionTestErrorCode;

    @Column(name = "last_model_sync_at")
    private Instant lastModelSyncAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "last_model_sync_status", length = 30)
    private ModelSyncStatus lastModelSyncStatus;

    @Column(name = "last_model_sync_error_code", length = 100)
    private String lastModelSyncErrorCode;

    @Column(name = "last_model_sync_discovered_count")
    private Integer lastModelSyncDiscoveredCount;

    @Column(name = "last_model_sync_created_count")
    private Integer lastModelSyncCreatedCount;

    @Column(name = "last_model_sync_updated_count")
    private Integer lastModelSyncUpdatedCount;

    @Column(name = "last_model_sync_unchanged_count")
    private Integer lastModelSyncUnchangedCount;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Integer version;

    public AiProvider() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public void setOrganizationId(UUID organizationId) {
        this.organizationId = organizationId;
    }

    public String getProviderKey() {
        return providerKey;
    }

    public void setProviderKey(String providerKey) {
        this.providerKey = providerKey;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public AiProviderType getProviderType() {
        return providerType;
    }

    public void setProviderType(AiProviderType providerType) {
        this.providerType = providerType;
    }

    public String getAdapterKey() {
        return adapterKey;
    }

    public void setAdapterKey(String adapterKey) {
        this.adapterKey = adapterKey;
    }

    public String getCredentialReference() {
        return credentialReference;
    }

    public void setCredentialReference(String credentialReference) {
        this.credentialReference = credentialReference;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public AiProviderStatus getStatus() {
        return status;
    }

    public void setStatus(AiProviderStatus status) {
        this.status = status;
    }

    public Integer getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(Integer requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public Integer getMaxConcurrentRequests() {
        return maxConcurrentRequests;
    }

    public void setMaxConcurrentRequests(Integer maxConcurrentRequests) {
        this.maxConcurrentRequests = maxConcurrentRequests;
    }

    public Integer getMaxRetries() {
        return maxRetries;
    }

    public void setMaxRetries(Integer maxRetries) {
        this.maxRetries = maxRetries;
    }

    public Integer getRetryBackoffMs() {
        return retryBackoffMs;
    }

    public void setRetryBackoffMs(Integer retryBackoffMs) {
        this.retryBackoffMs = retryBackoffMs;
    }

    public EndpointProfile getEndpointProfile() {
        return endpointProfile;
    }

    public void setEndpointProfile(EndpointProfile endpointProfile) {
        this.endpointProfile = endpointProfile;
    }

    public String getAzureResourceName() {
        return azureResourceName;
    }

    public void setAzureResourceName(String azureResourceName) {
        this.azureResourceName = azureResourceName;
    }

    public String getAzureApiVersion() {
        return azureApiVersion;
    }

    public void setAzureApiVersion(String azureApiVersion) {
        this.azureApiVersion = azureApiVersion;
    }

    public Instant getLastConnectionTestAt() {
        return lastConnectionTestAt;
    }

    public void setLastConnectionTestAt(Instant lastConnectionTestAt) {
        this.lastConnectionTestAt = lastConnectionTestAt;
    }

    public ConnectionTestStatus getLastConnectionTestStatus() {
        return lastConnectionTestStatus;
    }

    public void setLastConnectionTestStatus(ConnectionTestStatus lastConnectionTestStatus) {
        this.lastConnectionTestStatus = lastConnectionTestStatus;
    }

    public String getLastConnectionTestErrorCode() {
        return lastConnectionTestErrorCode;
    }

    public void setLastConnectionTestErrorCode(String lastConnectionTestErrorCode) {
        this.lastConnectionTestErrorCode = lastConnectionTestErrorCode;
    }

    public Instant getLastModelSyncAt() {
        return lastModelSyncAt;
    }

    public void setLastModelSyncAt(Instant lastModelSyncAt) {
        this.lastModelSyncAt = lastModelSyncAt;
    }

    public ModelSyncStatus getLastModelSyncStatus() {
        return lastModelSyncStatus;
    }

    public void setLastModelSyncStatus(ModelSyncStatus lastModelSyncStatus) {
        this.lastModelSyncStatus = lastModelSyncStatus;
    }

    public String getLastModelSyncErrorCode() {
        return lastModelSyncErrorCode;
    }

    public void setLastModelSyncErrorCode(String lastModelSyncErrorCode) {
        this.lastModelSyncErrorCode = lastModelSyncErrorCode;
    }

    public Integer getLastModelSyncDiscoveredCount() {
        return lastModelSyncDiscoveredCount;
    }

    public void setLastModelSyncDiscoveredCount(Integer lastModelSyncDiscoveredCount) {
        this.lastModelSyncDiscoveredCount = lastModelSyncDiscoveredCount;
    }

    public Integer getLastModelSyncCreatedCount() {
        return lastModelSyncCreatedCount;
    }

    public void setLastModelSyncCreatedCount(Integer lastModelSyncCreatedCount) {
        this.lastModelSyncCreatedCount = lastModelSyncCreatedCount;
    }

    public Integer getLastModelSyncUpdatedCount() {
        return lastModelSyncUpdatedCount;
    }

    public void setLastModelSyncUpdatedCount(Integer lastModelSyncUpdatedCount) {
        this.lastModelSyncUpdatedCount = lastModelSyncUpdatedCount;
    }

    public Integer getLastModelSyncUnchangedCount() {
        return lastModelSyncUnchangedCount;
    }

    public void setLastModelSyncUnchangedCount(Integer lastModelSyncUnchangedCount) {
        this.lastModelSyncUnchangedCount = lastModelSyncUnchangedCount;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }
}
