package ai.nova.platform.approval.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "approval_human_actions")
public class ApprovalHumanActionEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "task_id", nullable = false)
    private UUID taskId;

    @Column(name = "approval_decision_id", nullable = false)
    private UUID approvalDecisionId;

    @Column(name = "actor_user_id", nullable = false)
    private UUID actorUserId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ApprovalHumanActionType action;

    @Column(name = "comment_text", length = 2000)
    private String commentText;

    @Column(name = "evidence_fingerprint", nullable = false, length = 64)
    private String evidenceFingerprint;

    @Column(name = "idempotency_key", length = 100)
    private String idempotencyKey;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ApprovalHumanActionEntity() {
    }

    public ApprovalHumanActionEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID taskId,
            UUID approvalDecisionId,
            UUID actorUserId,
            ApprovalHumanActionType action,
            String commentText,
            String evidenceFingerprint,
            String idempotencyKey,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.taskId = taskId;
        this.approvalDecisionId = approvalDecisionId;
        this.actorUserId = actorUserId;
        this.action = action;
        this.commentText = commentText;
        this.evidenceFingerprint = evidenceFingerprint;
        this.idempotencyKey = idempotencyKey;
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

    public UUID getTaskId() {
        return taskId;
    }

    public UUID getApprovalDecisionId() {
        return approvalDecisionId;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public ApprovalHumanActionType getAction() {
        return action;
    }

    public String getCommentText() {
        return commentText;
    }

    public String getEvidenceFingerprint() {
        return evidenceFingerprint;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
