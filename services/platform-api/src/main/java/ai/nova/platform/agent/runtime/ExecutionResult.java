package ai.nova.platform.agent.runtime;

public record ExecutionResult(
        String responseText, int inputTokens, int outputTokens, int totalTokens, long latencyMs) {
}
