package ai.nova.platform.modelgateway.entity;

import java.math.BigDecimal;
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
@Table(name = "ai_models")
public class AiModel {

    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;
    @Column(name = "model_key", nullable = false, length = 100)
    private String modelKey;
    @Column(name = "provider_model_id", nullable = false)
    private String providerModelId;
    @Column(name = "display_name", nullable = false)
    private String displayName;
    @Column(length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false, length = 50)
    private AiModelType modelType;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AiModelStatus status;
    @Column(name = "context_window_tokens", nullable = false)
    private Integer contextWindowTokens;
    @Column(name = "max_output_tokens", nullable = false)
    private Integer maxOutputTokens;
    @Column(name = "supports_tools", nullable = false)
    private boolean supportsTools;
    @Column(name = "supports_knowledge_context", nullable = false)
    private boolean supportsKnowledgeContext;
    @Column(name = "supports_json_output", nullable = false)
    private boolean supportsJsonOutput;
    @Column(name = "supports_streaming", nullable = false)
    private boolean supportsStreaming;
    @Column(name = "supports_system_messages", nullable = false)
    private boolean supportsSystemMessages;
    @Column(name = "input_cost_per_million", precision = 18, scale = 8)
    private BigDecimal inputCostPerMillion;
    @Column(name = "output_cost_per_million", precision = 18, scale = 8)
    private BigDecimal outputCostPerMillion;
    @Column(name = "currency_code", length = 3)
    private String currencyCode;
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

    public AiModel() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public String getModelKey() { return modelKey; }
    public void setModelKey(String modelKey) { this.modelKey = modelKey; }
    public String getProviderModelId() { return providerModelId; }
    public void setProviderModelId(String providerModelId) { this.providerModelId = providerModelId; }
    public String getDisplayName() { return displayName; }
    public void setDisplayName(String displayName) { this.displayName = displayName; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public AiModelType getModelType() { return modelType; }
    public void setModelType(AiModelType modelType) { this.modelType = modelType; }
    public AiModelStatus getStatus() { return status; }
    public void setStatus(AiModelStatus status) { this.status = status; }
    public Integer getContextWindowTokens() { return contextWindowTokens; }
    public void setContextWindowTokens(Integer contextWindowTokens) { this.contextWindowTokens = contextWindowTokens; }
    public Integer getMaxOutputTokens() { return maxOutputTokens; }
    public void setMaxOutputTokens(Integer maxOutputTokens) { this.maxOutputTokens = maxOutputTokens; }
    public boolean isSupportsTools() { return supportsTools; }
    public void setSupportsTools(boolean supportsTools) { this.supportsTools = supportsTools; }
    public boolean isSupportsKnowledgeContext() { return supportsKnowledgeContext; }
    public void setSupportsKnowledgeContext(boolean supportsKnowledgeContext) { this.supportsKnowledgeContext = supportsKnowledgeContext; }
    public boolean isSupportsJsonOutput() { return supportsJsonOutput; }
    public void setSupportsJsonOutput(boolean supportsJsonOutput) { this.supportsJsonOutput = supportsJsonOutput; }
    public boolean isSupportsStreaming() { return supportsStreaming; }
    public void setSupportsStreaming(boolean supportsStreaming) { this.supportsStreaming = supportsStreaming; }
    public boolean isSupportsSystemMessages() { return supportsSystemMessages; }
    public void setSupportsSystemMessages(boolean supportsSystemMessages) { this.supportsSystemMessages = supportsSystemMessages; }
    public BigDecimal getInputCostPerMillion() { return inputCostPerMillion; }
    public void setInputCostPerMillion(BigDecimal inputCostPerMillion) { this.inputCostPerMillion = inputCostPerMillion; }
    public BigDecimal getOutputCostPerMillion() { return outputCostPerMillion; }
    public void setOutputCostPerMillion(BigDecimal outputCostPerMillion) { this.outputCostPerMillion = outputCostPerMillion; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }
    public UUID getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(UUID updatedBy) { this.updatedBy = updatedBy; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
}
