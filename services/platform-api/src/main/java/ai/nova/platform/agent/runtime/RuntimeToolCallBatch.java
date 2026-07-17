package ai.nova.platform.agent.runtime;

import java.util.List;

public record RuntimeToolCallBatch(List<RuntimeToolCallRequest> toolCalls) {
}
