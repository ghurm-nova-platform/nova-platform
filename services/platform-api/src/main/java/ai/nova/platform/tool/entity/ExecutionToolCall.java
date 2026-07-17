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
@Table(name = "execution_tool_calls")
public class ExecutionToolCall {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "agent_id", nullable = false)
    private UUID agentId;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "conversation_id")
    private UUID conversationId;

    @Column(name = "tool_id", nullable = false)
    private UUID toolId;

    @Column(name = "tool_key", nullable = false, length = 100)
    private String toolKey;

    @Column(name = "runtime_call_id", nullable = false, length = 100)
    private String runtimeCallId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ToolCallStatus status;

    @Column(name = "input_payload", nullable = false, columnDefinition = "TEXT")
    private String inputPayload;

    @Column(name = "output_payload", columnDefinition = "TEXT")
    private String outputPayload;

    @Column(name = "error_code", length = 100)
    private String errorCode;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "approved_by")
    private UUID approvedBy;

    @Column(name = "approved_at")
    private Instant approvedAt;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    protected ExecutionToolCall() {
    }

    public ExecutionToolCall(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID agentId,
            UUID executionId,
            UUID conversationId,
            UUID toolId,
            String toolKey,
            String runtimeCallId,
            int sequenceNumber,
            ToolCallStatus status,
            String inputPayload,
            UUID createdBy,
            Instant requestedAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.agentId = agentId;
        this.executionId = executionId;
        this.conversationId = conversationId;
        this.toolId = toolId;
        this.toolKey = toolKey;
        this.runtimeCallId = runtimeCallId;
        this.sequenceNumber = sequenceNumber;
        this.status = status;
        this.inputPayload = inputPayload;
        this.createdBy = createdBy;
        this.requestedAt = requestedAt;
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

    public UUID getAgentId() {
        return agentId;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public UUID getConversationId() {
        return conversationId;
    }

    public UUID getToolId() {
        return toolId;
    }

    public String getToolKey() {
        return toolKey;
    }

    public String getRuntimeCallId() {
        return runtimeCallId;
    }

    public int getSequenceNumber() {
        return sequenceNumber;
    }

    public ToolCallStatus getStatus() {
        return status;
    }

    public void setStatus(ToolCallStatus status) {
        this.status = status;
    }

    public String getInputPayload() {
        return inputPayload;
    }

    public String getOutputPayload() {
        return outputPayload;
    }

    public void setOutputPayload(String outputPayload) {
        this.outputPayload = outputPayload;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public Instant getRequestedAt() {
        return requestedAt;
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

    public Long getDurationMs() {
        return durationMs;
    }

    public void setDurationMs(Long durationMs) {
        this.durationMs = durationMs;
    }

    public UUID getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(UUID approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(Instant approvedAt) {
        this.approvedAt = approvedAt;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }
}
