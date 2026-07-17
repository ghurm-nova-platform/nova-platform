package ai.nova.platform.knowledge.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "agent_knowledge_assignments")
public class AgentKnowledgeAssignment {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "knowledge_base_id", nullable = false)
    private UUID knowledgeBaseId;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "top_k_override")
    private Integer topKOverride;

    @Column(name = "minimum_score_override", precision = 8, scale = 6)
    private BigDecimal minimumScoreOverride;

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

    protected AgentKnowledgeAssignment() {
    }

    public AgentKnowledgeAssignment(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID agentId,
            UUID knowledgeBaseId,
            boolean enabled,
            Integer topKOverride,
            BigDecimal minimumScoreOverride,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.agentId = agentId;
        this.knowledgeBaseId = knowledgeBaseId;
        this.enabled = enabled;
        this.topKOverride = topKOverride;
        this.minimumScoreOverride = minimumScoreOverride;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public UUID getAgentId() {
        return agentId;
    }

    public UUID getKnowledgeBaseId() {
        return knowledgeBaseId;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Integer getTopKOverride() {
        return topKOverride;
    }

    public void setTopKOverride(Integer topKOverride) {
        this.topKOverride = topKOverride;
    }

    public BigDecimal getMinimumScoreOverride() {
        return minimumScoreOverride;
    }

    public void setMinimumScoreOverride(BigDecimal minimumScoreOverride) {
        this.minimumScoreOverride = minimumScoreOverride;
    }

    public UUID getCreatedBy() {
        return createdBy;
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
