package ai.nova.platform.tool.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import ai.nova.platform.tool.entity.ToolCallStatus;
import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.entity.ToolType;

public final class ToolDtos {

    private ToolDtos() {
    }

    public record ToolCreateRequest(
            @NotBlank
            @Pattern(regexp = "^[A-Z][A-Z0-9_]*$", message = "toolKey must be uppercase snake case")
            @Size(max = 100)
            String toolKey,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @NotBlank @Size(max = 100) String executorKey,
            @NotBlank String inputSchema,
            String outputSchema,
            boolean requiresApproval,
            @Min(1) @Max(30) Integer maxExecutionSeconds,
            @Min(1) @Max(50000) Integer maxOutputCharacters) {
    }

    public record ToolUpdateRequest(
            @NotNull Integer version,
            @NotBlank @Size(max = 255) String name,
            @Size(max = 2000) String description,
            @Size(max = 100) String executorKey,
            @NotBlank String inputSchema,
            String outputSchema,
            boolean requiresApproval,
            @Min(1) @Max(30) Integer maxExecutionSeconds,
            @Min(1) @Max(50000) Integer maxOutputCharacters) {
    }

    public record ToolResponse(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String toolKey,
            String name,
            String description,
            ToolType toolType,
            String executorKey,
            String inputSchema,
            String outputSchema,
            ToolStatus status,
            boolean requiresApproval,
            int maxExecutionSeconds,
            int maxOutputCharacters,
            Integer version,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ExecutorKeysResponse(List<String> executorKeys) {
    }

    public record AgentToolAssignRequest(@NotNull UUID toolId) {
    }

    public record AgentToolAssignmentResponse(
            UUID id,
            UUID agentId,
            UUID toolId,
            String toolKey,
            String toolName,
            ToolStatus toolStatus,
            boolean enabled,
            Integer version,
            UUID createdBy,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ToolCallResponse(
            UUID id,
            UUID executionId,
            UUID agentId,
            UUID toolId,
            String toolKey,
            String runtimeCallId,
            int sequenceNumber,
            ToolCallStatus status,
            String inputPayload,
            String outputPayload,
            String errorCode,
            Instant requestedAt,
            Instant startedAt,
            Instant completedAt,
            Long durationMs,
            UUID approvedBy,
            Instant approvedAt,
            UUID createdBy) {
    }

    public record ToolCallApproveRequest(@NotNull Integer version) {
    }

    public record ToolCallRejectRequest(
            @NotNull Integer version,
            @NotBlank
            @Pattern(regexp = "^[A-Z][A-Z0-9_]{0,49}$", message = "reasonCode must be uppercase snake case")
            String reasonCode) {
    }

    public record ExecutionContinueResponse(
            UUID executionId,
            boolean readyToContinue,
            String message) {
    }
}
