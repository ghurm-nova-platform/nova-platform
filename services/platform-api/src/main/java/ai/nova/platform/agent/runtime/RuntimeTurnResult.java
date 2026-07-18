package ai.nova.platform.agent.runtime;

/**
 * Result of one runtime turn: either a final response or a batch of tool calls.
 */
public final class RuntimeTurnResult {

    private final RuntimeFinalResponse finalResponse;
    private final RuntimeToolCallBatch toolCallBatch;
    private final RuntimeModelMetadata modelMetadata;

    private RuntimeTurnResult(
            RuntimeFinalResponse finalResponse,
            RuntimeToolCallBatch toolCallBatch,
            RuntimeModelMetadata modelMetadata) {
        this.finalResponse = finalResponse;
        this.toolCallBatch = toolCallBatch;
        this.modelMetadata = modelMetadata;
    }

    public static RuntimeTurnResult finalResponse(RuntimeFinalResponse response) {
        return new RuntimeTurnResult(response, null, null);
    }

    public static RuntimeTurnResult finalResponse(RuntimeFinalResponse response, RuntimeModelMetadata modelMetadata) {
        return new RuntimeTurnResult(response, null, modelMetadata);
    }

    public static RuntimeTurnResult toolCalls(RuntimeToolCallBatch batch) {
        return new RuntimeTurnResult(null, batch, null);
    }

    public static RuntimeTurnResult toolCalls(RuntimeToolCallBatch batch, RuntimeModelMetadata modelMetadata) {
        return new RuntimeTurnResult(null, batch, modelMetadata);
    }

    public boolean isFinal() {
        return finalResponse != null;
    }

    public boolean isToolCalls() {
        return toolCallBatch != null && toolCallBatch.toolCalls() != null && !toolCallBatch.toolCalls().isEmpty();
    }

    public RuntimeFinalResponse finalResponse() {
        return finalResponse;
    }

    public RuntimeToolCallBatch toolCallBatch() {
        return toolCallBatch;
    }

    public RuntimeModelMetadata modelMetadata() {
        return modelMetadata;
    }
}
