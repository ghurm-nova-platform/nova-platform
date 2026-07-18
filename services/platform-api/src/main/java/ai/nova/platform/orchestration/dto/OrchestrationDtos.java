package ai.nova.platform.orchestration.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.orchestration.entity.AttemptStatus;
import ai.nova.platform.orchestration.entity.DependencyType;
import ai.nova.platform.orchestration.entity.ExecutionMode;
import ai.nova.platform.orchestration.entity.FailurePolicy;
import ai.nova.platform.orchestration.entity.OrchestrationEventType;
import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;

public final class OrchestrationDtos {

    private OrchestrationDtos() {
    }

    public record CreateRunRequest(
            @NotNull UUID projectId,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 4000) String objective,
            @NotNull ExecutionMode executionMode,
            @NotNull FailurePolicy failurePolicy,
            @Min(1) @Max(100) Integer maxParallelTasks,
            @Min(1000) @Max(86400000) Long maximumDurationMs,
            UUID initiatedByAgentId,
            @Size(max = 100000) String inputJson,
            @Size(max = 100000) String metadataJson) {
    }

    public record UpdateRunRequest(
            @NotNull Long version,
            @NotBlank @Size(max = 255) String name,
            @NotBlank @Size(max = 4000) String objective,
            @NotNull ExecutionMode executionMode,
            @NotNull FailurePolicy failurePolicy,
            @Min(1) @Max(100) Integer maxParallelTasks,
            @Min(1000) @Max(86400000) Long maximumDurationMs,
            UUID initiatedByAgentId,
            @Size(max = 100000) String inputJson,
            @Size(max = 100000) String metadataJson) {
    }

    public record CancelRunRequest(@Size(max = 500) String reason) {
    }

    public record CreateTaskRequest(
            @NotBlank @Size(max = 150) String taskKey,
            @NotBlank @Size(max = 255) String displayName,
            @Size(max = 2000) String description,
            @NotNull TaskType taskType,
            UUID assignedAgentId,
            @Size(max = 150) String modelReference,
            @Size(max = 100000) String requiredCapabilitiesJson,
            @Size(max = 100000) String inputJson,
            @Min(1) @Max(20) Integer maxAttempts,
            @Min(0) @Max(3600000) Long retryBackoffMs,
            @Min(1) @Max(1000) Integer priority,
            @Min(1) @Max(3600) Integer timeoutSeconds,
            Integer sequenceOrder,
            @Size(max = 150) String idempotencyKey) {
    }

    public record UpdateTaskRequest(
            @NotNull Long version,
            @NotBlank @Size(max = 150) String taskKey,
            @NotBlank @Size(max = 255) String displayName,
            @Size(max = 2000) String description,
            @NotNull TaskType taskType,
            UUID assignedAgentId,
            @Size(max = 150) String modelReference,
            @Size(max = 100000) String requiredCapabilitiesJson,
            @Size(max = 100000) String inputJson,
            @Min(1) @Max(20) Integer maxAttempts,
            @Min(0) @Max(3600000) Long retryBackoffMs,
            @Min(1) @Max(1000) Integer priority,
            @Min(1) @Max(3600) Integer timeoutSeconds,
            Integer sequenceOrder,
            @Size(max = 150) String idempotencyKey) {
    }

    public record CreateDependencyRequest(
            @NotNull UUID predecessorTaskId,
            @NotNull UUID successorTaskId,
            @NotNull DependencyType dependencyType) {
    }

    public record DeleteDependencyRequest(
            @NotNull UUID predecessorTaskId, @NotNull UUID successorTaskId) {
    }

    public record RunResponse(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID initiatedByAgentId,
            UUID rootExecutionId,
            String name,
            String objective,
            RunStatus status,
            ExecutionMode executionMode,
            FailurePolicy failurePolicy,
            int maxParallelTasks,
            long maximumDurationMs,
            Instant startedAt,
            Instant completedAt,
            Instant cancelledAt,
            Instant deadlineAt,
            String cancellationReason,
            String failureCode,
            String failureMessage,
            String inputJson,
            String outputJson,
            String metadataJson,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt,
            Long version,
            Map<String, Long> taskStatusCounts,
            long runningTaskCount,
            double completedPercentage) {
    }

    public record TaskResponse(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID runId,
            String taskKey,
            String displayName,
            String description,
            TaskType taskType,
            TaskStatus status,
            UUID assignedAgentId,
            String modelReference,
            String requiredCapabilitiesJson,
            String inputJson,
            String outputJson,
            String errorCode,
            String errorMessage,
            int attemptCount,
            int maxAttempts,
            long retryBackoffMs,
            Instant nextAttemptAt,
            int priority,
            int timeoutSeconds,
            Integer sequenceOrder,
            String idempotencyKey,
            Instant startedAt,
            Instant completedAt,
            Instant cancelledAt,
            Instant claimedAt,
            String claimedBy,
            Instant claimExpiresAt,
            UUID createdBy,
            UUID updatedBy,
            Instant createdAt,
            Instant updatedAt,
            Long version) {
    }

    public record DependencyResponse(
            UUID runId,
            UUID predecessorTaskId,
            UUID successorTaskId,
            DependencyType dependencyType,
            Instant createdAt) {
    }

    public record GraphResponse(UUID runId, List<TaskResponse> nodes, List<DependencyResponse> edges) {
    }

    public record EventResponse(
            UUID id,
            UUID runId,
            UUID taskId,
            OrchestrationEventType eventType,
            long eventSequence,
            String payloadJson,
            UUID createdBy,
            Instant createdAt) {
    }

    public record AttemptResponse(
            UUID id,
            UUID runId,
            UUID taskId,
            int attemptNumber,
            AttemptStatus status,
            UUID executionId,
            Instant startedAt,
            Instant completedAt,
            Long durationMs,
            String inputSnapshotJson,
            String outputSnapshotJson,
            String errorCode,
            String errorMessage,
            boolean retryable,
            String workerId,
            Instant createdAt) {
    }
}
