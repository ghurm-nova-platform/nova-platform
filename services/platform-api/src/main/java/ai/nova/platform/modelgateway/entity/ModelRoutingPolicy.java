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
@Table(name = "model_routing_policies")
public class ModelRoutingPolicy {

    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "agent_id")
    private UUID agentId;
    @Column(name = "policy_key", nullable = false, length = 100)
    private String policyKey;
    @Column(nullable = false)
    private String name;
    @Column(length = 2000)
    private String description;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoutingPolicyStatus status;
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private RoutingStrategy strategy;
    @Column(name = "fallback_enabled", nullable = false)
    private boolean fallbackEnabled;
    @Column(name = "retry_enabled", nullable = false)
    private boolean retryEnabled;
    @Column(name = "maximum_provider_attempts", nullable = false)
    private Integer maximumProviderAttempts;
    @Column(name = "maximum_total_duration_ms", nullable = false)
    private Long maximumTotalDurationMs;
    @Column(name = "require_tool_support", nullable = false)
    private boolean requireToolSupport;
    @Column(name = "require_knowledge_support", nullable = false)
    private boolean requireKnowledgeSupport;
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

    public ModelRoutingPolicy() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public String getPolicyKey() { return policyKey; }
    public void setPolicyKey(String policyKey) { this.policyKey = policyKey; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public RoutingPolicyStatus getStatus() { return status; }
    public void setStatus(RoutingPolicyStatus status) { this.status = status; }
    public RoutingStrategy getStrategy() { return strategy; }
    public void setStrategy(RoutingStrategy strategy) { this.strategy = strategy; }
    public boolean isFallbackEnabled() { return fallbackEnabled; }
    public void setFallbackEnabled(boolean fallbackEnabled) { this.fallbackEnabled = fallbackEnabled; }
    public boolean isRetryEnabled() { return retryEnabled; }
    public void setRetryEnabled(boolean retryEnabled) { this.retryEnabled = retryEnabled; }
    public Integer getMaximumProviderAttempts() { return maximumProviderAttempts; }
    public void setMaximumProviderAttempts(Integer maximumProviderAttempts) { this.maximumProviderAttempts = maximumProviderAttempts; }
    public Long getMaximumTotalDurationMs() { return maximumTotalDurationMs; }
    public void setMaximumTotalDurationMs(Long maximumTotalDurationMs) { this.maximumTotalDurationMs = maximumTotalDurationMs; }
    public boolean isRequireToolSupport() { return requireToolSupport; }
    public void setRequireToolSupport(boolean requireToolSupport) { this.requireToolSupport = requireToolSupport; }
    public boolean isRequireKnowledgeSupport() { return requireKnowledgeSupport; }
    public void setRequireKnowledgeSupport(boolean requireKnowledgeSupport) { this.requireKnowledgeSupport = requireKnowledgeSupport; }
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
