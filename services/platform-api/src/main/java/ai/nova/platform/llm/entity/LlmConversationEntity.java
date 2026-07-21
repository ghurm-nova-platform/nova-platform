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
@Table(name = "llm_conversations")
public class LlmConversationEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "model_id")
    private UUID modelId;

    @Column(length = 500)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LlmConversationStatus status;

    private String summary;

    @Column(name = "token_usage_input", nullable = false)
    private int tokenUsageInput;

    @Column(name = "token_usage_output", nullable = false)
    private int tokenUsageOutput;

    @Column(name = "metadata_json", nullable = false)
    private String metadataJson;

    @Version
    @Column(nullable = false)
    private int version;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected LlmConversationEntity() {
    }

    public LlmConversationEntity(
            UUID id, UUID organizationId, UUID userId, UUID modelId, String title, Instant now) {
        this.id = id;
        this.organizationId = organizationId;
        this.userId = userId;
        this.modelId = modelId;
        this.title = title;
        this.status = LlmConversationStatus.ACTIVE;
        this.tokenUsageInput = 0;
        this.tokenUsageOutput = 0;
        this.metadataJson = "{}";
        this.createdAt = now;
        this.updatedAt = now;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getProjectId() { return projectId; }
    public UUID getUserId() { return userId; }
    public UUID getModelId() { return modelId; }
    public String getTitle() { return title; }
    public LlmConversationStatus getStatus() { return status; }
    public String getSummary() { return summary; }
    public int getTokenUsageInput() { return tokenUsageInput; }
    public int getTokenUsageOutput() { return tokenUsageOutput; }
    public String getMetadataJson() { return metadataJson; }
    public int getVersion() { return version; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public void setProjectId(UUID projectId) { this.projectId = projectId; }
    public void setModelId(UUID modelId) { this.modelId = modelId; }
    public void setTitle(String title) { this.title = title; }
    public void setStatus(LlmConversationStatus status) { this.status = status; }
    public void setSummary(String summary) { this.summary = summary; }
    public void setTokenUsageInput(int tokenUsageInput) { this.tokenUsageInput = tokenUsageInput; }
    public void setTokenUsageOutput(int tokenUsageOutput) { this.tokenUsageOutput = tokenUsageOutput; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public void touch(Instant now) { this.updatedAt = now; }
}
