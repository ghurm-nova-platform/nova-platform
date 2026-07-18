package ai.nova.platform.modelgateway.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "project_models")
public class ProjectModel {

    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "model_id", nullable = false)
    private UUID modelId;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "is_default", nullable = false)
    private boolean isDefault;
    @Column(name = "maximum_input_tokens_override")
    private Integer maximumInputTokensOverride;
    @Column(name = "maximum_output_tokens_override")
    private Integer maximumOutputTokensOverride;
    @Column(name = "daily_request_limit")
    private Integer dailyRequestLimit;
    @Column(name = "monthly_request_limit")
    private Integer monthlyRequestLimit;
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

    public ProjectModel() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public boolean isDefault() { return isDefault; }
    public void setDefault(boolean isDefault) { this.isDefault = isDefault; }
    public Integer getMaximumInputTokensOverride() { return maximumInputTokensOverride; }
    public void setMaximumInputTokensOverride(Integer maximumInputTokensOverride) { this.maximumInputTokensOverride = maximumInputTokensOverride; }
    public Integer getMaximumOutputTokensOverride() { return maximumOutputTokensOverride; }
    public void setMaximumOutputTokensOverride(Integer maximumOutputTokensOverride) { this.maximumOutputTokensOverride = maximumOutputTokensOverride; }
    public Integer getDailyRequestLimit() { return dailyRequestLimit; }
    public void setDailyRequestLimit(Integer dailyRequestLimit) { this.dailyRequestLimit = dailyRequestLimit; }
    public Integer getMonthlyRequestLimit() { return monthlyRequestLimit; }
    public void setMonthlyRequestLimit(Integer monthlyRequestLimit) { this.monthlyRequestLimit = monthlyRequestLimit; }
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
