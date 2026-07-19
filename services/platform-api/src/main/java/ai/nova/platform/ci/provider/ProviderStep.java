package ai.nova.platform.ci.provider;

import java.time.Instant;
import java.util.List;

public record ProviderStep(
        int stepNumber,
        String stepName,
        String status,
        String conclusion,
        Long durationMs,
        String failureReason,
        Instant startedAt,
        Instant completedAt) {
}
