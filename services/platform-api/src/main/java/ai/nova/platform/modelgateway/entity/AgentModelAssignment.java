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
@Table(name = "agent_model_assignments")
public class AgentModelAssignment {

    @Id
    private UUID id;
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    @Column(name = "project_id", nullable = false)
    private UUID projectId;
    @Column(name = "agent_id", nullable = false)
    private UUID agentId;
    @Column(name = "model_id", nullable = false)
    private UUID modelId;
    @Column(nullable = false)
    private Integer priority;
    @Enumerated(EnumType.STRING)
    @Column(name = "assignment_role", nullable = false, length = 30)
    private AssignmentRole assignmentRole;
    @Column(nullable = false)
    private boolean enabled;
    @Column(name = "temperature_override", precision = 5, scale = 4)
    private BigDecimal temperatureOverride;
    @Column(name = "maximum_output_tokens_override")
    private Integer maximumOutputTokensOverride;
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

    public AgentModelAssignment() {
    }

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getOrganizationId() { return organizationId; }
    public void setOrganizationId(UUID organizationId) { this.organizationId = organizationId; }
    public UUID getProjectId() { return projectId; }
    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public UUID getAgentId() { return agentId; }
    public void setAgentId(UUID agentId) { this.agentId = agentId; }
    public UUID getModelId() { return modelId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public AssignmentRole getAssignmentRole() { return assignmentRole; }
    public void setAssignmentRole(AssignmentRole assignmentRole) { this.assignmentRole = assignmentRole; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public BigDecimal getTemperatureOverride() { return temperatureOverride; }
    public void setTemperatureOverride(BigDecimal temperatureOverride) { this.temperatureOverride = temperatureOverride; }
    public Integer getMaximumOutputTokensOverride() { return maximumOutputTokensOverride; }
    public void setMaximumOutputTokensOverride(Integer maximumOutputTokensOverride) { this.maximumOutputTokensOverride = maximumOutputTokensOverride; }
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
