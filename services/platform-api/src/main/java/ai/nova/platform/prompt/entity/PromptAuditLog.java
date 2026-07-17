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
@Table(name = "prompt_audit_log")
public class PromptAuditLog {

    @Id
    private UUID id;

    @Column(name = "prompt_id", nullable = false)
    private UUID promptId;

    @Column(name = "prompt_version_id")
    private UUID promptVersionId;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PromptAuditAction action;

    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    protected PromptAuditLog() {
    }

    public PromptAuditLog(
            UUID id,
            UUID promptId,
            UUID promptVersionId,
            UUID organizationId,
            UUID projectId,
            PromptAuditAction action,
            String oldValue,
            String newValue,
            UUID performedBy,
            Instant performedAt,
            String correlationId) {
        this.id = id;
        this.promptId = promptId;
        this.promptVersionId = promptVersionId;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.action = action;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.correlationId = correlationId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPromptId() {
        return promptId;
    }

    public PromptAuditAction getAction() {
        return action;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }
}
