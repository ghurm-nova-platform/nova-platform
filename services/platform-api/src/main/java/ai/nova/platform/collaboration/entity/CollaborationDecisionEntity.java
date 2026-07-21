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
@Table(name = "collaboration_decisions")
public class CollaborationDecisionEntity {

    @Id
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "decision_type", nullable = false, length = 40)
    private CollaborationDecisionType decisionType;

    @Column(name = "summary", nullable = false, length = 500)
    private String summary;

    @Column(name = "details_json", columnDefinition = "TEXT")
    private String detailsJson;

    @Column(name = "decided_by")
    private UUID decidedBy;

    @Column(name = "task_id")
    private UUID taskId;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CollaborationDecisionEntity() {
    }

    public CollaborationDecisionEntity(
            UUID id,
            UUID sessionId,
            UUID organizationId,
            CollaborationDecisionType decisionType,
            String summary,
            String detailsJson,
            UUID decidedBy,
            UUID taskId,
            Instant createdAt) {
        this.id = id;
        this.sessionId = sessionId;
        this.organizationId = organizationId;
        this.decisionType = decisionType;
        this.summary = summary;
        this.detailsJson = detailsJson;
        this.decidedBy = decidedBy;
        this.taskId = taskId;
        this.createdAt = createdAt;
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

    public CollaborationDecisionType getDecisionType() {
        return decisionType;
    }

    public String getSummary() {
        return summary;
    }

    public String getDetailsJson() {
        return detailsJson;
    }

    public UUID getDecidedBy() {
        return decidedBy;
    }

    public UUID getTaskId() {
        return taskId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
