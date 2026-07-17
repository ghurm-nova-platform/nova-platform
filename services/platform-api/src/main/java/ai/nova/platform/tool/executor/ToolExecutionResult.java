package ai.nova.platform.tool.executor;

import com.fasterxml.jackson.databind.JsonNode;

public record ToolExecutionResult(
        ToolExecutionOutcome status,
        JsonNode output,
        String errorCode,
        long durationMs) {

    public static ToolExecutionResult success(JsonNode output, long durationMs) {
        return new ToolExecutionResult(ToolExecutionOutcome.SUCCESS, output, null, durationMs);
    }

    public static ToolExecutionResult failure(String errorCode, long durationMs) {
        return new ToolExecutionResult(ToolExecutionOutcome.FAILED, null, errorCode, durationMs);
    }
}
