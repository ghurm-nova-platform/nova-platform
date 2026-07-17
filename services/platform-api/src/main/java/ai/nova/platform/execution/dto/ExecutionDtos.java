package ai.nova.platform.execution.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.entity.MessageRole;

public final class ExecutionDtos {

    private ExecutionDtos() {
    }

    public record ExecuteInput(@NotBlank String message) {
    }

    public record ExecuteRequest(
            @NotNull @Valid ExecuteInput input,
            Map<String, String> variables,
            UUID conversationId,
            UUID clientRequestId) {
    }

    public record TokenUsage(int input, int output, int total) {
    }

    public record ExecuteResponse(
            UUID executionId,
            ExecutionStatus status,
            String response,
            long latencyMs,
            TokenUsage tokens,
            String renderedPrompt,
            String errorMessage,
            Boolean awaitingApproval,
            UUID pendingToolCallId) {
    }

    public record ExecutionSummaryResponse(
            UUID id,
            UUID agentId,
            UUID promptVersionId,
            UUID conversationId,
            String provider,
            String model,
            ExecutionStatus status,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            Integer latencyMs,
            Instant startedAt,
            Instant completedAt,
            UUID createdBy,
            Instant createdAt,
            String errorMessage) {
    }

    public record ExecutionMessageResponse(MessageRole role, String content, Instant createdAt) {
    }

    public record ExecutionDetailResponse(
            UUID id,
            UUID agentId,
            UUID promptVersionId,
            UUID conversationId,
            String provider,
            String model,
            ExecutionStatus status,
            Integer inputTokens,
            Integer outputTokens,
            Integer totalTokens,
            Integer latencyMs,
            Instant startedAt,
            Instant completedAt,
            UUID createdBy,
            Instant createdAt,
            String errorMessage,
            List<ExecutionMessageResponse> messages) {
    }
}
