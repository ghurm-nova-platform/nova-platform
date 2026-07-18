package ai.nova.platform.knowledge.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "execution_knowledge_snapshots")
public class ExecutionKnowledgeSnapshot {

    @Id
    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "snapshot_json", nullable = false, columnDefinition = "TEXT")
    private String snapshotJson;

    @Column(name = "citation_count", nullable = false)
    private int citationCount;

    @Column(name = "total_characters", nullable = false)
    private int totalCharacters;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExecutionKnowledgeSnapshot() {
    }

    public ExecutionKnowledgeSnapshot(
            UUID executionId,
            UUID organizationId,
            UUID projectId,
            String snapshotJson,
            int citationCount,
            int totalCharacters,
            Instant createdAt) {
        this.executionId = executionId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.snapshotJson = snapshotJson;
        this.citationCount = citationCount;
        this.totalCharacters = totalCharacters;
        this.createdAt = createdAt;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getSnapshotJson() {
        return snapshotJson;
    }

    public int getCitationCount() {
        return citationCount;
    }

    public int getTotalCharacters() {
        return totalCharacters;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
