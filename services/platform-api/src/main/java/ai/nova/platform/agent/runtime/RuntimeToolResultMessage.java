package ai.nova.platform.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Tool result returned to the runtime after Platform API execution.
 */
public record RuntimeToolResultMessage(
        String runtimeCallId,
        String toolKey,
        String status,
        JsonNode output,
        String errorCode) {
}
