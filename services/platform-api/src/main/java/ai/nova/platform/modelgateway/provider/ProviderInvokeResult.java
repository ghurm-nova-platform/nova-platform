package ai.nova.platform.modelgateway.provider;

import java.util.List;

import ai.nova.platform.agent.runtime.RuntimeToolCallRequest;

public record ProviderInvokeResult(
        ProviderInvokeOutcome outcome,
        String responseText,
        List<RuntimeToolCallRequest> toolCalls,
        int inputTokens,
        int outputTokens,
        long latencyMs,
        String finishReason,
        String providerRequestId,
        String errorCode,
        ProviderFailureKind failureKind) {

    public static ProviderInvokeResult finalResponse(
            String responseText, int inputTokens, int outputTokens, long latencyMs, String finishReason) {
        return new ProviderInvokeResult(
                ProviderInvokeOutcome.FINAL,
                responseText,
                List.of(),
                inputTokens,
                outputTokens,
                latencyMs,
                finishReason,
                null,
                null,
                null);
    }

    public static ProviderInvokeResult toolCalls(List<RuntimeToolCallRequest> toolCalls) {
        return new ProviderInvokeResult(
                ProviderInvokeOutcome.TOOL_CALLS,
                null,
                toolCalls,
                0,
                0,
                0L,
                "tool_calls",
                null,
                null,
                null);
    }

    public static ProviderInvokeResult failure(String errorCode, ProviderFailureKind kind) {
        return new ProviderInvokeResult(
                ProviderInvokeOutcome.FAILURE,
                null,
                List.of(),
                0,
                0,
                0L,
                null,
                null,
                errorCode,
                kind);
    }
}
