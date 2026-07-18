package ai.nova.platform.modelgateway.gateway;

import org.springframework.stereotype.Component;

import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeModelMetadata;
import ai.nova.platform.agent.runtime.RuntimeToolCallBatch;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.modelgateway.provider.ProviderInvokeOutcome;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;

@Component
public class ModelGatewayRuntimeMapper {

    public RuntimeTurnResult toTurnResult(ProviderInvokeResult result, RuntimeModelMetadata metadata) {
        if (result.outcome() == ProviderInvokeOutcome.FINAL) {
            RuntimeFinalResponse finalResponse = new RuntimeFinalResponse(
                    result.responseText(),
                    result.inputTokens(),
                    result.outputTokens(),
                    result.inputTokens() + result.outputTokens(),
                    result.latencyMs());
            return RuntimeTurnResult.finalResponse(finalResponse, metadata);
        }
        if (result.outcome() == ProviderInvokeOutcome.TOOL_CALLS) {
            return RuntimeTurnResult.toolCalls(
                    new RuntimeToolCallBatch(result.toolCalls()), metadata);
        }
        throw new IllegalStateException("Cannot map failed provider result to runtime turn");
    }

    public RuntimeModelMetadata toMetadata(ModelGatewayResponse response) {
        return new RuntimeModelMetadata(
                response.providerId(),
                response.providerName(),
                response.modelId(),
                response.modelName(),
                response.fallbackUsed(),
                response.attemptCount());
    }
}
