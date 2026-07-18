package ai.nova.platform.orchestration.mapper;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import ai.nova.platform.orchestration.dto.OrchestrationDtos.AttemptResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.DependencyResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.EventResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.GraphResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.RunResponse;
import ai.nova.platform.orchestration.dto.OrchestrationDtos.TaskResponse;
import ai.nova.platform.orchestration.entity.AgentOrchestrationEvent;
import ai.nova.platform.orchestration.entity.AgentOrchestrationRun;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.AgentTaskAttempt;
import ai.nova.platform.orchestration.entity.AgentTaskDependency;
import ai.nova.platform.orchestration.entity.TaskStatus;

@Component
public class OrchestrationMapper {

    public RunResponse toRunResponse(AgentOrchestrationRun run, List<AgentOrchestrationTask> tasks) {
        Map<String, Long> counts = new LinkedHashMap<>();
        for (TaskStatus s : TaskStatus.values()) {
            counts.put(s.name(), 0L);
        }
        long terminal = 0;
        long running = 0;
        long total = tasks == null ? 0 : tasks.size();
        if (tasks != null) {
            for (AgentOrchestrationTask task : tasks) {
                counts.merge(task.getStatus().name(), 1L, Long::sum);
                if (isTerminal(task.getStatus())) {
                    terminal++;
                }
                if (task.getStatus() == TaskStatus.RUNNING
                        || task.getStatus() == TaskStatus.CLAIMED
                        || task.getStatus() == TaskStatus.READY) {
                    running++;
                }
            }
        }
        double pct = total == 0 ? 0.0 : (100.0 * terminal / total);
        return new RunResponse(
                run.getId(),
                run.getOrganizationId(),
                run.getProjectId(),
                run.getInitiatedByAgentId(),
                run.getRootExecutionId(),
                run.getName(),
                run.getObjective(),
                run.getStatus(),
                run.getExecutionMode(),
                run.getFailurePolicy(),
                run.getMaxParallelTasks(),
                run.getMaximumDurationMs(),
                run.getStartedAt(),
                run.getCompletedAt(),
                run.getCancelledAt(),
                run.getDeadlineAt(),
                run.getCancellationReason(),
                run.getFailureCode(),
                run.getFailureMessage(),
                run.getInputJson(),
                run.getOutputJson(),
                run.getMetadataJson(),
                run.getCreatedBy(),
                run.getUpdatedBy(),
                run.getCreatedAt(),
                run.getUpdatedAt(),
                run.getVersion(),
                counts,
                running,
                pct);
    }

    public TaskResponse toTaskResponse(AgentOrchestrationTask task) {
        return new TaskResponse(
                task.getId(),
                task.getOrganizationId(),
                task.getProjectId(),
                task.getRunId(),
                task.getTaskKey(),
                task.getDisplayName(),
                task.getDescription(),
                task.getTaskType(),
                task.getStatus(),
                task.getAssignedAgentId(),
                task.getModelReference(),
                task.getRequiredCapabilitiesJson(),
                task.getInputJson(),
                task.getOutputJson(),
                task.getErrorCode(),
                task.getErrorMessage(),
                task.getAttemptCount(),
                task.getMaxAttempts(),
                task.getRetryBackoffMs(),
                task.getNextAttemptAt(),
                task.getPriority(),
                task.getTimeoutSeconds(),
                task.getSequenceOrder(),
                task.getIdempotencyKey(),
                task.getStartedAt(),
                task.getCompletedAt(),
                task.getCancelledAt(),
                task.getClaimedAt(),
                task.getClaimedBy(),
                task.getClaimExpiresAt(),
                task.getCreatedBy(),
                task.getUpdatedBy(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                task.getVersion());
    }

    public DependencyResponse toDependencyResponse(AgentTaskDependency dep) {
        return new DependencyResponse(
                dep.getRunId(),
                dep.getPredecessorTaskId(),
                dep.getSuccessorTaskId(),
                dep.getDependencyType(),
                dep.getCreatedAt());
    }

    public GraphResponse toGraphResponse(
            java.util.UUID runId, List<AgentOrchestrationTask> tasks, List<AgentTaskDependency> deps) {
        return new GraphResponse(
                runId,
                tasks.stream().map(this::toTaskResponse).toList(),
                deps.stream().map(this::toDependencyResponse).toList());
    }

    public EventResponse toEventResponse(AgentOrchestrationEvent event) {
        return new EventResponse(
                event.getId(),
                event.getRunId(),
                event.getTaskId(),
                event.getEventType(),
                event.getEventSequence(),
                event.getPayloadJson(),
                event.getCreatedBy(),
                event.getCreatedAt());
    }

    public AttemptResponse toAttemptResponse(AgentTaskAttempt attempt) {
        return new AttemptResponse(
                attempt.getId(),
                attempt.getRunId(),
                attempt.getTaskId(),
                attempt.getAttemptNumber(),
                attempt.getStatus(),
                attempt.getExecutionId(),
                attempt.getStartedAt(),
                attempt.getCompletedAt(),
                attempt.getDurationMs(),
                attempt.getInputSnapshotJson(),
                attempt.getOutputSnapshotJson(),
                attempt.getErrorCode(),
                attempt.getErrorMessage(),
                attempt.isRetryable(),
                attempt.getWorkerId(),
                attempt.getCreatedAt());
    }

    private static boolean isTerminal(TaskStatus status) {
        return status == TaskStatus.SUCCEEDED
                || status == TaskStatus.FAILED
                || status == TaskStatus.SKIPPED
                || status == TaskStatus.CANCELLED
                || status == TaskStatus.TIMED_OUT;
    }
}
