package ai.nova.platform.modelgateway.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "model_usage_daily")
public class ModelUsageDaily {

    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "provider_id", nullable = false)
    private UUID providerId;
    @Column(name = "model_id", nullable = false)
    private UUID modelId;
    @Column(name = "usage_date", nullable = false)
    private LocalDate usageDate;
    @Column(name = "request_count", nullable = false)
    private Long requestCount;
    @Column(name = "successful_request_count", nullable = false)
    private Long successfulRequestCount;
    @Column(name = "failed_request_count", nullable = false)
    private Long failedRequestCount;
    @Column(name = "input_tokens", nullable = false)
    private Long inputTokens;
    @Column(name = "output_tokens", nullable = false)
    private Long outputTokens;
    @Column(name = "estimated_cost", precision = 20, scale = 8)
    private BigDecimal estimatedCost;
    @Column(name = "currency_code", length = 3)
    private String currencyCode;
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public ModelUsageDaily() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getProviderId() { return providerId; }
    public void setProviderId(UUID providerId) { this.providerId = providerId; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public LocalDate getUsageDate() { return usageDate; }
    public void setUsageDate(LocalDate usageDate) { this.usageDate = usageDate; }
    public Long getRequestCount() { return requestCount; }
    public void setRequestCount(Long requestCount) { this.requestCount = requestCount; }
    public Long getSuccessfulRequestCount() { return successfulRequestCount; }
    public void setSuccessfulRequestCount(Long successfulRequestCount) { this.successfulRequestCount = successfulRequestCount; }
    public Long getFailedRequestCount() { return failedRequestCount; }
    public void setFailedRequestCount(Long failedRequestCount) { this.failedRequestCount = failedRequestCount; }
    public Long getInputTokens() { return inputTokens; }
    public void setInputTokens(Long inputTokens) { this.inputTokens = inputTokens; }
    public Long getOutputTokens() { return outputTokens; }
    public void setOutputTokens(Long outputTokens) { this.outputTokens = outputTokens; }
    public BigDecimal getEstimatedCost() { return estimatedCost; }
    public void setEstimatedCost(BigDecimal estimatedCost) { this.estimatedCost = estimatedCost; }
    public String getCurrencyCode() { return currencyCode; }
    public void setCurrencyCode(String currencyCode) { this.currencyCode = currencyCode; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
