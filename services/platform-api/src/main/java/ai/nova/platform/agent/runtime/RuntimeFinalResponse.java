package ai.nova.platform.agent.runtime;

/**
 * Final assistant response from a runtime turn.
 */
public record RuntimeFinalResponse(
        String responseText,
        int inputTokens,
        int outputTokens,
        int totalTokens,
        long latencyMs) {
}
