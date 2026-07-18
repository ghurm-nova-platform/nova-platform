package ai.nova.platform.coding.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "generated_artifacts")
public class GeneratedArtifact {

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

    @Enumerated(EnumType.STRING)
    @Column(name = "artifact_type", nullable = false, length = 50)
    private ArtifactType artifactType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ArtifactLanguage language;

    @Column(nullable = false, length = 1000)
    private String path;

    @Column(nullable = false)
    private String filename;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false, length = 64)
    private String sha256;

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

    protected GeneratedArtifact() {
    }

    public GeneratedArtifact(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID runId,
            UUID taskId,
            ArtifactType artifactType,
            ArtifactLanguage language,
            String path,
            String filename,
            String content,
            String sha256,
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
        this.artifactType = artifactType;
        this.language = language;
        this.path = path;
        this.filename = filename;
        this.content = content;
        this.sha256 = sha256;
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

    public ArtifactType getArtifactType() {
        return artifactType;
    }

    public ArtifactLanguage getLanguage() {
        return language;
    }

    public String getPath() {
        return path;
    }

    public String getFilename() {
        return filename;
    }

    public String getContent() {
        return content;
    }

    public String getSha256() {
        return sha256;
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
