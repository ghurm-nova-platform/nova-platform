package ai.nova.platform.agent.runtime;

import java.util.UUID;

/**
 * Safe model metadata surfaced to execution clients (no credentials or provider bodies).
 */
public record RuntimeModelMetadata(
        UUID providerId,
        String providerName,
        UUID modelId,
        String modelName,
        boolean fallbackUsed,
        int attemptCount) {
}
