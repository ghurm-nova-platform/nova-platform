package ai.nova.platform.tool.service;

import java.time.Instant;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.tool.entity.ToolAuditAction;
import ai.nova.platform.tool.entity.ToolAuditLog;
import ai.nova.platform.tool.repository.ToolAuditLogRepository;
import ai.nova.platform.web.correlation.CorrelationIdFilter;

@Service
public class ToolAuditService {

    private final ToolAuditLogRepository auditLogRepository;

    public ToolAuditService(ToolAuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void write(
            ToolAuditAction action,
            UUID organizationId,
            UUID projectId,
            UUID toolId,
            UUID agentId,
            UUID executionId,
            UUID toolCallId,
            String metadata,
            UUID performedBy) {
        auditLogRepository.save(new ToolAuditLog(
                UUID.randomUUID(),
                organizationId,
                projectId,
                toolId,
                agentId,
                executionId,
                toolCallId,
                action,
                sanitizeMetadata(metadata),
                performedBy,
                Instant.now(),
                MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY)));
    }

    public void toolCreated(ToolDefinitionContext ctx, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_CREATED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                null,
                null,
                null,
                safeJson("toolKey", ctx.toolKey(), "status", ctx.status()),
                performedBy);
    }

    public void toolUpdated(ToolDefinitionContext ctx, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_UPDATED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                null,
                null,
                null,
                safeJson("toolKey", ctx.toolKey(), "status", ctx.status()),
                performedBy);
    }

    public void toolActivated(ToolDefinitionContext ctx, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_ACTIVATED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                null,
                null,
                null,
                safeJson("toolKey", ctx.toolKey()),
                performedBy);
    }

    public void toolArchived(ToolDefinitionContext ctx, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_ARCHIVED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                null,
                null,
                null,
                safeJson("toolKey", ctx.toolKey()),
                performedBy);
    }

    public void toolAssigned(
            UUID organizationId, UUID projectId, UUID toolId, UUID agentId, String toolKey, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_ASSIGNED,
                organizationId,
                projectId,
                toolId,
                agentId,
                null,
                null,
                safeJson("toolKey", toolKey, "agentId", agentId.toString()),
                performedBy);
    }

    public void toolUnassigned(
            UUID organizationId, UUID projectId, UUID toolId, UUID agentId, String toolKey, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_UNASSIGNED,
                organizationId,
                projectId,
                toolId,
                agentId,
                null,
                null,
                safeJson("toolKey", toolKey, "agentId", agentId.toString()),
                performedBy);
    }

    public void toolCallRequested(ToolCallContext ctx, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_CALL_REQUESTED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                ctx.agentId(),
                ctx.executionId(),
                ctx.toolCallId(),
                safeJson(
                        "toolKey", ctx.toolKey(),
                        "runtimeCallId", ctx.runtimeCallId(),
                        "sequenceNumber", String.valueOf(ctx.sequenceNumber()),
                        "status", ctx.status()),
                performedBy);
    }

    public void toolCallApproved(ToolCallContext ctx, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_CALL_APPROVED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                ctx.agentId(),
                ctx.executionId(),
                ctx.toolCallId(),
                safeJson("toolKey", ctx.toolKey(), "runtimeCallId", ctx.runtimeCallId()),
                performedBy);
    }

    public void toolCallRejected(ToolCallContext ctx, String reasonCode, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_CALL_REJECTED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                ctx.agentId(),
                ctx.executionId(),
                ctx.toolCallId(),
                safeJson("toolKey", ctx.toolKey(), "runtimeCallId", ctx.runtimeCallId(), "reasonCode", reasonCode),
                performedBy);
    }

    public void toolCallStarted(ToolCallContext ctx, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_CALL_STARTED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                ctx.agentId(),
                ctx.executionId(),
                ctx.toolCallId(),
                safeJson("toolKey", ctx.toolKey(), "runtimeCallId", ctx.runtimeCallId()),
                performedBy);
    }

    public void toolCallCompleted(ToolCallContext ctx, long durationMs, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_CALL_COMPLETED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                ctx.agentId(),
                ctx.executionId(),
                ctx.toolCallId(),
                safeJson(
                        "toolKey", ctx.toolKey(),
                        "runtimeCallId", ctx.runtimeCallId(),
                        "durationMs", String.valueOf(durationMs)),
                performedBy);
    }

    public void toolCallFailed(ToolCallContext ctx, String errorCode, long durationMs, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_CALL_FAILED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                ctx.agentId(),
                ctx.executionId(),
                ctx.toolCallId(),
                safeJson(
                        "toolKey", ctx.toolKey(),
                        "runtimeCallId", ctx.runtimeCallId(),
                        "errorCode", errorCode,
                        "durationMs", String.valueOf(durationMs)),
                performedBy);
    }

    public void toolOutputTruncated(ToolCallContext ctx, UUID performedBy) {
        write(
                ToolAuditAction.TOOL_OUTPUT_TRUNCATED,
                ctx.organizationId(),
                ctx.projectId(),
                ctx.toolId(),
                ctx.agentId(),
                ctx.executionId(),
                ctx.toolCallId(),
                safeJson("toolKey", ctx.toolKey(), "runtimeCallId", ctx.runtimeCallId()),
                performedBy);
    }

    private String sanitizeMetadata(String metadata) {
        if (metadata == null) {
            return null;
        }
        String lower = metadata.toLowerCase();
        if (lower.contains("input") || lower.contains("output") || lower.contains("payload")) {
            throw new IllegalArgumentException("Audit metadata must not contain input/output content");
        }
        return metadata;
    }

    private String safeJson(String... keyValues) {
        StringBuilder builder = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            if (i > 0) {
                builder.append(',');
            }
            builder.append('"').append(escape(keyValues[i])).append("\":\"").append(escape(keyValues[i + 1])).append('"');
        }
        builder.append('}');
        return builder.toString();
    }

    private String escape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    public record ToolDefinitionContext(UUID organizationId, UUID projectId, UUID toolId, String toolKey, String status) {
    }

    public record ToolCallContext(
            UUID organizationId,
            UUID projectId,
            UUID toolId,
            UUID agentId,
            UUID executionId,
            UUID toolCallId,
            String toolKey,
            String runtimeCallId,
            int sequenceNumber,
            String status) {
    }
}
