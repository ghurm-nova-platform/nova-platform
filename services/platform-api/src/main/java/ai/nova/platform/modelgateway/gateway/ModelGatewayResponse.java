package ai.nova.platform.modelgateway.gateway;

import java.util.UUID;

import ai.nova.platform.agent.runtime.RuntimeTurnResult;

public record ModelGatewayResponse(
        RuntimeTurnResult turnResult,
        UUID providerId,
        String providerName,
        UUID modelId,
        String modelName,
        boolean fallbackUsed,
        int attemptCount) {
}
