package ai.nova.platform.ci.provider;

import java.time.Instant;

public record ProviderWorkflowRun(
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
        Instant startedAt,
        Instant completedAt) {
}
