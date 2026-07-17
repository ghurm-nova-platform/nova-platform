package ai.nova.platform.agent.runtime;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Provider-neutral tool call request returned by the runtime.
 */
public record RuntimeToolCallRequest(
        String runtimeCallId,
        String toolKey,
        JsonNode arguments) {
}
