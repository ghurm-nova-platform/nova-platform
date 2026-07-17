package ai.nova.platform.agent.runtime;

/**
 * Result of one runtime turn: either a final response or a batch of tool calls.
 */
public final class RuntimeTurnResult {

    private final RuntimeFinalResponse finalResponse;
    private final RuntimeToolCallBatch toolCallBatch;

    private RuntimeTurnResult(RuntimeFinalResponse finalResponse, RuntimeToolCallBatch toolCallBatch) {
        this.finalResponse = finalResponse;
        this.toolCallBatch = toolCallBatch;
    }

    public static RuntimeTurnResult finalResponse(RuntimeFinalResponse response) {
        return new RuntimeTurnResult(response, null);
    }

    public static RuntimeTurnResult toolCalls(RuntimeToolCallBatch batch) {
        return new RuntimeTurnResult(null, batch);
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
}
