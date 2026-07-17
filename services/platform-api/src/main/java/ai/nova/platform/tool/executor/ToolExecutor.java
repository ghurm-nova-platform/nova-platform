package ai.nova.platform.tool.executor;

import com.fasterxml.jackson.databind.JsonNode;

public interface ToolExecutor {

    String executorKey();

    ToolExecutionResult execute(ToolExecutionContext context, JsonNode input);
}
