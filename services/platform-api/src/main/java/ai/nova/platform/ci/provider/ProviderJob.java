package ai.nova.platform.ci.provider;

import java.time.Instant;
import java.util.List;

public record ProviderJob(
        String externalJobId,
        String jobName,
        String status,
        String conclusion,
        Long durationMs,
        String failureReason,
        List<ProviderStep> steps,
        Instant startedAt,
        Instant completedAt) {
}
