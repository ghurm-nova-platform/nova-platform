package ai.nova.platform.llm;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.llm.gateway.LLMGateway;
import ai.nova.platform.llm.provider.LlmCompletionRequest;
import ai.nova.platform.llm.provider.LlmCompletionRequest.LlmChatMessage;
import ai.nova.platform.llm.provider.LlmCompletionResult;
import ai.nova.platform.modelgateway.provider.AiModelProvider;
import ai.nova.platform.modelgateway.provider.ProviderCapabilities;
import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.ProviderFailureKind;
import ai.nova.platform.modelgateway.provider.ProviderInvokeRequest;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.security.AuthenticatedUser;

/**
 * Bridge so the cloud model gateway can route invocations to the local LLM runtime
 * without modules calling Ollama (or other local providers) directly.
 */
@Component
public class LocalLlmAiModelProvider implements AiModelProvider {

    public static final String ADAPTER_KEY = "LOCAL_LLM";

    private static final UUID DEMO_ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");

    private final LLMGateway llmGateway;

    public LocalLlmAiModelProvider(LLMGateway llmGateway) {
        this.llmGateway = llmGateway;
    }

    @Override
    public String adapterKey() {
        return ADAPTER_KEY;
    }

    @Override
    public ProviderCapabilities capabilities() {
        return new ProviderCapabilities(false, true, true, true, false);
    }

    @Override
    public ProviderInvokeResult invoke(ProviderInvokeRequest request) throws ProviderException {
        long start = System.nanoTime();
        try {
            UUID organizationId = resolveOrganizationId();
            List<LlmChatMessage> messages = new ArrayList<>();
            if (request.systemPrompt() != null && !request.systemPrompt().isBlank()) {
                messages.add(new LlmChatMessage("system", request.systemPrompt()));
            }
            if (request.messages() != null) {
                for (RuntimeMessage message : request.messages()) {
                    messages.add(new LlmChatMessage(
                            message.role() == null ? "user" : message.role().toLowerCase(),
                            message.content() == null ? "" : message.content()));
                }
            }
            LlmCompletionRequest completionRequest = new LlmCompletionRequest(
                    request.providerModelId(),
                    messages,
                    request.maxOutputTokens() > 0 ? request.maxOutputTokens() : null,
                    null,
                    false);
            LlmCompletionResult result =
                    llmGateway.completeByModelCode(organizationId, request.providerModelId(), completionRequest);
            long latencyMs = (System.nanoTime() - start) / 1_000_000L;
            return ProviderInvokeResult.finalResponse(
                    result.content(),
                    result.inputTokens(),
                    result.outputTokens(),
                    latencyMs > 0 ? latencyMs : result.latencyMs(),
                    result.finishReason() == null ? "stop" : result.finishReason());
        } catch (Exception ex) {
            throw new ProviderException(
                    "PROVIDER_ERROR",
                    ProviderFailureKind.TRANSIENT,
                    "LOCAL_LLM invoke failed: " + ex.getMessage());
        }
    }

    private static UUID resolveOrganizationId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user) {
            return user.getOrganizationId();
        }
        return DEMO_ORG_ID;
    }
}
