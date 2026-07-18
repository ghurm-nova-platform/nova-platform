package ai.nova.platform.testing.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "testing_results")
public class TestingResultEntity {

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

    @Column(name = "coverage_estimate", nullable = false)
    private int coverageEstimate;

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

    protected TestingResultEntity() {
    }

    public TestingResultEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID runId,
            UUID taskId,
            String summary,
            int coverageEstimate,
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
        this.coverageEstimate = coverageEstimate;
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

    public int getCoverageEstimate() {
        return coverageEstimate;
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
