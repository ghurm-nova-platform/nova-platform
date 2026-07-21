package ai.nova.platform.collaboration.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "collaboration_sessions")
public class CollaborationSessionEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "orchestration_run_id")
    private UUID orchestrationRunId;

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CollaborationSessionStatus status;

    @Column(name = "shared_context_json", nullable = false, columnDefinition = "TEXT")
    private String sharedContextJson;

    @Column(name = "parallel_group", length = 80)
    private String parallelGroup;

    @Column(name = "conflict_detected", nullable = false)
    private boolean conflictDetected;

    @Column(name = "conflict_details_json", columnDefinition = "TEXT")
    private String conflictDetailsJson;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CollaborationSessionEntity() {
    }

    public CollaborationSessionEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID orchestrationRunId,
            String name,
            CollaborationSessionStatus status,
            String sharedContextJson,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.orchestrationRunId = orchestrationRunId;
        this.name = name;
        this.status = status;
        this.sharedContextJson = sharedContextJson;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
        this.conflictDetected = false;
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

    public UUID getOrchestrationRunId() {
        return orchestrationRunId;
    }

    public void setOrchestrationRunId(UUID orchestrationRunId) {
        this.orchestrationRunId = orchestrationRunId;
    }

    public String getName() {
        return name;
    }

    public CollaborationSessionStatus getStatus() {
        return status;
    }

    public void setStatus(CollaborationSessionStatus status) {
        this.status = status;
    }

    public String getSharedContextJson() {
        return sharedContextJson;
    }

    public void setSharedContextJson(String sharedContextJson) {
        this.sharedContextJson = sharedContextJson;
    }

    public String getParallelGroup() {
        return parallelGroup;
    }

    public void setParallelGroup(String parallelGroup) {
        this.parallelGroup = parallelGroup;
    }

    public boolean isConflictDetected() {
        return conflictDetected;
    }

    public void setConflictDetected(boolean conflictDetected) {
        this.conflictDetected = conflictDetected;
    }

    public String getConflictDetailsJson() {
        return conflictDetailsJson;
    }

    public void setConflictDetailsJson(String conflictDetailsJson) {
        this.conflictDetailsJson = conflictDetailsJson;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
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
}
