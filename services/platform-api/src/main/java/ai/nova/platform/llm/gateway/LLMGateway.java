package ai.nova.platform.llm.gateway;

import java.util.UUID;

import ai.nova.platform.llm.entity.LlmModelEntity;
import ai.nova.platform.llm.provider.LlmCompletionRequest;
import ai.nova.platform.llm.provider.LlmCompletionResult;

public interface LLMGateway {

    LlmCompletionResult complete(UUID organizationId, LlmModelEntity model, LlmCompletionRequest request);

    LlmCompletionResult completeByModelCode(UUID organizationId, String modelCode, LlmCompletionRequest request);
}
