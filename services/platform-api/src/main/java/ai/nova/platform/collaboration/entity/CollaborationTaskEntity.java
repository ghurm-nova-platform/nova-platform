package ai.nova.platform.collaboration.entity;

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
@Table(name = "collaboration_tasks")
public class CollaborationTaskEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "participant_id")
    private UUID participantId;

    @Column(name = "task_key", nullable = false, length = 120)
    private String taskKey;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CollaborationTaskStatus status;

    @Column(name = "depends_on_task_id")
    private UUID dependsOnTaskId;

    @Column(name = "blocked_by_task_id")
    private UUID blockedByTaskId;

    @Column(name = "completed_by_participant_id")
    private UUID completedByParticipantId;

    @Column(name = "artifact_ref", length = 500)
    private String artifactRef;

    @Column(name = "parallel_group", length = 80)
    private String parallelGroup;

    @Column(name = "assigned_at")
    private Instant assignedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    protected CollaborationTaskEntity() {
    }

    public CollaborationTaskEntity(
            UUID id,
            UUID sessionId,
            UUID organizationId,
            String taskKey,
            String title,
            CollaborationTaskStatus status,
            UUID dependsOnTaskId,
            String artifactRef,
            String parallelGroup,
            Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.organizationId = organizationId;
        this.taskKey = taskKey;
        this.title = title;
        this.status = status;
        this.dependsOnTaskId = dependsOnTaskId;
        this.artifactRef = artifactRef;
        this.parallelGroup = parallelGroup;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getParticipantId() {
        return participantId;
    }

    public void setParticipantId(UUID participantId) {
        this.participantId = participantId;
    }

    public String getTaskKey() {
        return taskKey;
    }

    public String getTitle() {
        return title;
    }

    public CollaborationTaskStatus getStatus() {
        return status;
    }

    public void setStatus(CollaborationTaskStatus status) {
        this.status = status;
    }

    public UUID getDependsOnTaskId() {
        return dependsOnTaskId;
    }

    public UUID getBlockedByTaskId() {
        return blockedByTaskId;
    }

    public void setBlockedByTaskId(UUID blockedByTaskId) {
        this.blockedByTaskId = blockedByTaskId;
    }

    public UUID getCompletedByParticipantId() {
        return completedByParticipantId;
    }

    public void setCompletedByParticipantId(UUID completedByParticipantId) {
        this.completedByParticipantId = completedByParticipantId;
    }

    public String getArtifactRef() {
        return artifactRef;
    }

    public void setArtifactRef(String artifactRef) {
        this.artifactRef = artifactRef;
    }

    public String getParallelGroup() {
        return parallelGroup;
    }

    public void setParallelGroup(String parallelGroup) {
        this.parallelGroup = parallelGroup;
    }

    public Instant getAssignedAt() {
        return assignedAt;
    }

    public void setAssignedAt(Instant assignedAt) {
        this.assignedAt = assignedAt;
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

    public long getVersion() {
        return version;
    }
}
