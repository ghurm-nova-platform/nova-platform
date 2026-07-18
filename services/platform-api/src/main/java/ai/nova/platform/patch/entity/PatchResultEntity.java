package ai.nova.platform.patch.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "patch_results")
public class PatchResultEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(nullable = false, length = 4000)
    private String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PatchStatus status;

    @Column(name = "files_changed", nullable = false)
    private int filesChanged;

    @Column(nullable = false)
    private int insertions;

    @Column(nullable = false)
    private int deletions;

    @Column(name = "patch_size", nullable = false)
    private int patchSize;

    @Column(name = "patch_content", nullable = false, columnDefinition = "TEXT")
    private String patchContent;

    @Column(name = "validation_message", length = 2000)
    private String validationMessage;

    @Column(name = "tokens_used")
    private Long tokensUsed;

    @Column(length = 150)
    private String model;

    @Column(length = 100)
    private String provider;

    @Column(name = "generation_time_ms")
    private Long generationTimeMs;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PatchResultEntity() {
    }

    public PatchResultEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID runId,
            UUID taskId,
            String summary,
            PatchStatus status,
            int filesChanged,
            int insertions,
            int deletions,
            int patchSize,
            String patchContent,
            String validationMessage,
            Long tokensUsed,
            String model,
            String provider,
            Long generationTimeMs,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.runId = runId;
        this.taskId = taskId;
        this.summary = summary;
        this.status = status;
        this.filesChanged = filesChanged;
        this.insertions = insertions;
        this.deletions = deletions;
        this.patchSize = patchSize;
        this.patchContent = patchContent;
        this.validationMessage = validationMessage;
        this.tokensUsed = tokensUsed;
        this.model = model;
        this.provider = provider;
        this.generationTimeMs = generationTimeMs;
        this.createdAt = createdAt;
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

    public UUID getRunId() {
        return runId;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public String getSummary() {
        return summary;
    }

    public PatchStatus getStatus() {
        return status;
    }

    public int getFilesChanged() {
        return filesChanged;
    }

    public int getInsertions() {
        return insertions;
    }

    public int getDeletions() {
        return deletions;
    }

    public int getPatchSize() {
        return patchSize;
    }

    public String getPatchContent() {
        return patchContent;
    }

    public String getValidationMessage() {
        return validationMessage;
    }

    public Long getTokensUsed() {
        return tokensUsed;
    }

    public String getModel() {
        return model;
    }

    public String getProvider() {
        return provider;
    }

    public Long getGenerationTimeMs() {
        return generationTimeMs;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
