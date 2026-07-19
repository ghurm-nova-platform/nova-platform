package ai.nova.platform.ci.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.ci.entity.CiObservationStatus;
import ai.nova.platform.ci.entity.CiOverallStatus;

public final class CiDtos {

    private CiDtos() {
    }

    public record CiRunRequest(@NotNull UUID taskId) {
    }

    public record TimelineEvent(String phase, Instant at, String detail) {
    }

    public record CiStep(
            UUID id,
            int stepNumber,
            String stepName,
            String status,
            String conclusion,
            Long durationMs,
            String failureReason,
            Instant startedAt,
            Instant completedAt) {
    }

    public record CiJob(
            UUID id,
            String externalJobId,
            String jobName,
            String status,
            String conclusion,
            Long durationMs,
            String failureReason,
            List<CiStep> steps,
            Instant startedAt,
            Instant completedAt) {
    }

    public record CiWorkflowRun(
            UUID id,
            String externalWorkflowId,
            String workflowName,
            String externalRunId,
            String runUrl,
            String status,
            String conclusion,
            Long durationMs,
            String triggerEvent,
            String commitHash,
            String branch,
            Long pullRequestNumber,
            String failureReason,
            List<CiJob> jobs,
            Instant startedAt,
            Instant completedAt) {
    }

    public record CiFailureSummary(
            int failedWorkflows,
            int failedJobs,
            int failedSteps,
            long durationMs,
            List<String> errorMessages,
            List<String> affectedFiles) {
    }

    public record CiObservationOperation(
            UUID id,
            UUID taskId,
            UUID projectId,
            UUID pullRequestOperationId,
            CiObservationStatus status,
            String provider,
            String repositoryOwner,
            String repositoryName,
            String sourceBranch,
            String targetBranch,
            String commitHash,
            Long pullRequestNumber,
            CiOverallStatus overallStatus,
            CiFailureSummary failureSummary,
            String retryRecommendation,
            List<CiWorkflowRun> workflows,
            String errorCode,
            String errorMessage,
            List<TimelineEvent> timeline,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
    }
}
