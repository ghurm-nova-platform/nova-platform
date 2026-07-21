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
@Table(name = "collaboration_participants")
public class CollaborationParticipantEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_role", nullable = false, length = 40)
    private CollaborationParticipantRole participantRole;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private CollaborationParticipantStatus status;

    @Column(name = "current_task_id")
    private UUID currentTaskId;

    @Column(name = "progress_percent", nullable = false)
    private int progressPercent;

    @Column(name = "parallel_group", length = 80)
    private String parallelGroup;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected CollaborationParticipantEntity() {
    }

    public CollaborationParticipantEntity(
            UUID id,
            UUID sessionId,
            UUID organizationId,
            CollaborationParticipantRole participantRole,
            CollaborationParticipantStatus status,
            Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.organizationId = organizationId;
        this.participantRole = participantRole;
        this.status = status;
        this.progressPercent = 0;
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

    public CollaborationParticipantRole getParticipantRole() {
        return participantRole;
    }

    public CollaborationParticipantStatus getStatus() {
        return status;
    }

    public void setStatus(CollaborationParticipantStatus status) {
        this.status = status;
    }

    public UUID getCurrentTaskId() {
        return currentTaskId;
    }

    public void setCurrentTaskId(UUID currentTaskId) {
        this.currentTaskId = currentTaskId;
    }

    public int getProgressPercent() {
        return progressPercent;
    }

    public void setProgressPercent(int progressPercent) {
        this.progressPercent = progressPercent;
    }

    public String getParallelGroup() {
        return parallelGroup;
    }

    public void setParallelGroup(String parallelGroup) {
        this.parallelGroup = parallelGroup;
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
