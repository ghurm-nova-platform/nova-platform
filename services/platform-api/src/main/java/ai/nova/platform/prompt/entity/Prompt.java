package ai.nova.platform.prompt.entity;

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
@Table(name = "prompts")
public class Prompt {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(nullable = false)
    private String name;

    @Column(length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "prompt_type", nullable = false, length = 50)
    private PromptType promptType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromptStatus status;

    @Column(name = "current_draft_version_id")
    private UUID currentDraftVersionId;

    @Column(name = "published_version_id")
    private UUID publishedVersionId;

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

    protected Prompt() {
    }

    public Prompt(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String name,
            String description,
            PromptType promptType,
            PromptStatus status,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.name = name;
        this.description = description;
        this.promptType = promptType;
        this.status = status;
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

    public PromptType getPromptType() {
        return promptType;
    }

    public void setPromptType(PromptType promptType) {
        this.promptType = promptType;
    }

    public PromptStatus getStatus() {
        return status;
    }

    public void setStatus(PromptStatus status) {
        this.status = status;
    }

    public UUID getCurrentDraftVersionId() {
        return currentDraftVersionId;
    }

    public void setCurrentDraftVersionId(UUID currentDraftVersionId) {
        this.currentDraftVersionId = currentDraftVersionId;
    }

    public UUID getPublishedVersionId() {
        return publishedVersionId;
    }

    public void setPublishedVersionId(UUID publishedVersionId) {
        this.publishedVersionId = publishedVersionId;
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
