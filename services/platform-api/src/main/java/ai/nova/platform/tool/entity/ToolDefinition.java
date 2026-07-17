package ai.nova.platform.tool.entity;

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
@Table(name = "tools")
public class ToolDefinition {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "tool_key", nullable = false, length = 100)
    private String toolKey;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "tool_type", nullable = false, length = 30)
    private ToolType toolType;

    @Column(name = "executor_key", nullable = false, length = 100)
    private String executorKey;

    @Column(name = "input_schema", nullable = false, columnDefinition = "TEXT")
    private String inputSchema;

    @Column(name = "output_schema", columnDefinition = "TEXT")
    private String outputSchema;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ToolStatus status;

    @Column(name = "requires_approval", nullable = false)
    private boolean requiresApproval;

    @Column(name = "max_execution_seconds", nullable = false)
    private int maxExecutionSeconds;

    @Column(name = "max_output_characters", nullable = false)
    private int maxOutputCharacters;

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

    protected ToolDefinition() {
    }

    public ToolDefinition(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String toolKey,
            String name,
            String description,
            ToolType toolType,
            String executorKey,
            String inputSchema,
            String outputSchema,
            ToolStatus status,
            boolean requiresApproval,
            int maxExecutionSeconds,
            int maxOutputCharacters,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.toolKey = toolKey;
        this.name = name;
        this.description = description;
        this.toolType = toolType;
        this.executorKey = executorKey;
        this.inputSchema = inputSchema;
        this.outputSchema = outputSchema;
        this.status = status;
        this.requiresApproval = requiresApproval;
        this.maxExecutionSeconds = maxExecutionSeconds;
        this.maxOutputCharacters = maxOutputCharacters;
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

    public String getToolKey() {
        return toolKey;
    }

    public void setToolKey(String toolKey) {
        this.toolKey = toolKey;
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

    public ToolType getToolType() {
        return toolType;
    }

    public void setToolType(ToolType toolType) {
        this.toolType = toolType;
    }

    public String getExecutorKey() {
        return executorKey;
    }

    public void setExecutorKey(String executorKey) {
        this.executorKey = executorKey;
    }

    public String getInputSchema() {
        return inputSchema;
    }

    public void setInputSchema(String inputSchema) {
        this.inputSchema = inputSchema;
    }

    public String getOutputSchema() {
        return outputSchema;
    }

    public void setOutputSchema(String outputSchema) {
        this.outputSchema = outputSchema;
    }

    public ToolStatus getStatus() {
        return status;
    }

    public void setStatus(ToolStatus status) {
        this.status = status;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public int getMaxExecutionSeconds() {
        return maxExecutionSeconds;
    }

    public void setMaxExecutionSeconds(int maxExecutionSeconds) {
        this.maxExecutionSeconds = maxExecutionSeconds;
    }

    public int getMaxOutputCharacters() {
        return maxOutputCharacters;
    }

    public void setMaxOutputCharacters(int maxOutputCharacters) {
        this.maxOutputCharacters = maxOutputCharacters;
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
