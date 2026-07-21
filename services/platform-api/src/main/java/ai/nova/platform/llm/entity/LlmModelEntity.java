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
@Table(name = "llm_models")
public class LlmModelEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(nullable = false, length = 120)
    private String code;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 80)
    private LlmModelFamily family;

    @Enumerated(EnumType.STRING)
    @Column(name = "provider_type", nullable = false, length = 40)
    private LlmProviderType providerType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LlmModelStatus status;

    @Column(name = "capabilities_json", nullable = false)
    private String capabilitiesJson;

    @Column(name = "tags_json", nullable = false)
    private String tagsJson;

    private String owner;

    @Column(name = "context_length", nullable = false)
    private int contextLength;

    private String tokenizer;

    @Column(name = "memory_mb")
    private Integer memoryMb;

    @Column(name = "gpu_required", nullable = false)
    private boolean gpuRequired;

    @Column(name = "cpu_cores")
    private Integer cpuCores;

    @Column(name = "endpoint_url", length = 1000)
    private String endpointUrl;

    @Column(name = "metadata_json", nullable = false)
    private String metadataJson;

    @Column(nullable = false)
    private boolean enabled;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LlmModelEntity() {
    }

    public LlmModelEntity(
            UUID id,
            UUID organizationId,
            String code,
            String displayName,
            LlmModelFamily family,
            LlmProviderType providerType,
            LlmModelStatus status,
            Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.code = code;
        this.displayName = displayName;
        this.family = family;
        this.providerType = providerType;
        this.status = status;
        this.capabilitiesJson = "[]";
        this.tagsJson = "[]";
        this.contextLength = 4096;
        this.gpuRequired = false;
        this.metadataJson = "{}";
        this.enabled = true;
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public String getCode() { return code; }
    public String getDisplayName() { return displayName; }
    public LlmModelFamily getFamily() { return family; }
    public LlmProviderType getProviderType() { return providerType; }
    public LlmModelStatus getStatus() { return status; }
    public String getCapabilitiesJson() { return capabilitiesJson; }
    public String getTagsJson() { return tagsJson; }
    public String getOwner() { return owner; }
    public int getContextLength() { return contextLength; }
    public String getTokenizer() { return tokenizer; }
    public Integer getMemoryMb() { return memoryMb; }
    public boolean isGpuRequired() { return gpuRequired; }
    public Integer getCpuCores() { return cpuCores; }
    public String getEndpointUrl() { return endpointUrl; }
    public String getMetadataJson() { return metadataJson; }
    public boolean isEnabled() { return enabled; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public void setFamily(LlmModelFamily family) { this.family = family; }
    public void setProviderType(LlmProviderType providerType) { this.providerType = providerType; }
    public void setStatus(LlmModelStatus status) { this.status = status; }
    public void setCapabilitiesJson(String capabilitiesJson) { this.capabilitiesJson = capabilitiesJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public void setOwner(String owner) { this.owner = owner; }
    public void setContextLength(int contextLength) { this.contextLength = contextLength; }
    public void setTokenizer(String tokenizer) { this.tokenizer = tokenizer; }
    public void setMemoryMb(Integer memoryMb) { this.memoryMb = memoryMb; }
    public void setGpuRequired(boolean gpuRequired) { this.gpuRequired = gpuRequired; }
    public void setCpuCores(Integer cpuCores) { this.cpuCores = cpuCores; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public void touch(Instant now) { this.updatedAt = now; }
}
