package ai.nova.platform.prompt.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompt_versions")
public class PromptVersion {

    @Id
    private UUID id;

    @Column(name = "prompt_id", nullable = false)
    private UUID promptId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "version_number", nullable = false)
    private Integer versionNumber;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "change_summary", length = 1000)
    private String changeSummary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromptVersionStatus status;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "published_by")
    private UUID publishedBy;

    @Column(name = "published_at")
    private Instant publishedAt;

    protected PromptVersion() {
    }

    public PromptVersion(
            UUID id,
            UUID promptId,
            UUID organizationId,
            UUID projectId,
            Integer versionNumber,
            String content,
            String changeSummary,
            PromptVersionStatus status,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.promptId = promptId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.versionNumber = versionNumber;
        this.content = content;
        this.changeSummary = changeSummary;
        this.status = status;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPromptId() {
        return promptId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public Integer getVersionNumber() {
        return versionNumber;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getChangeSummary() {
        return changeSummary;
    }

    public void setChangeSummary(String changeSummary) {
        this.changeSummary = changeSummary;
    }

    public PromptVersionStatus getStatus() {
        return status;
    }

    public void setStatus(PromptVersionStatus status) {
        this.status = status;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public UUID getPublishedBy() {
        return publishedBy;
    }

    public void setPublishedBy(UUID publishedBy) {
        this.publishedBy = publishedBy;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public void setPublishedAt(Instant publishedAt) {
        this.publishedAt = publishedAt;
    }
}
