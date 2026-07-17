package ai.nova.platform.tool.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tool_audit_log")
public class ToolAuditLog {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id")
    private UUID projectId;

    @Column(name = "tool_id")
    private UUID toolId;

    @Column(name = "agent_id")
    private UUID agentId;

    @Column(name = "execution_id")
    private UUID executionId;

    @Column(name = "tool_call_id")
    private UUID toolCallId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private ToolAuditAction action;

    @Column(columnDefinition = "TEXT")
    private String metadata;

    @Column(name = "performed_by", nullable = false)
    private UUID performedBy;

    @Column(name = "performed_at", nullable = false)
    private Instant performedAt;

    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    protected ToolAuditLog() {
    }

    public ToolAuditLog(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID toolId,
            UUID agentId,
            UUID executionId,
            UUID toolCallId,
            ToolAuditAction action,
            String metadata,
            UUID performedBy,
            Instant performedAt,
            String correlationId) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.toolId = toolId;
        this.agentId = agentId;
        this.executionId = executionId;
        this.toolCallId = toolCallId;
        this.action = action;
        this.metadata = metadata;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.correlationId = correlationId;
    }

    public UUID getId() {
        return id;
    }

    public ToolAuditAction getAction() {
        return action;
    }

    public String getMetadata() {
        return metadata;
    }
}
