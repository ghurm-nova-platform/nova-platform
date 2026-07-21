package ai.nova.platform.llm.service;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.llm.dto.LlmDtos.BatchCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.ChatCompletionRequest;
import ai.nova.platform.llm.dto.LlmDtos.ChatMessageDto;
import ai.nova.platform.llm.dto.LlmDtos.CompletionResponse;
import ai.nova.platform.llm.dto.LlmDtos.TextCompletionRequest;
import ai.nova.platform.llm.entity.LlmModelEntity;
import ai.nova.platform.llm.entity.LlmModelStatus;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.gateway.LLMGateway;
import ai.nova.platform.llm.provider.LlmCompletionRequest;
import ai.nova.platform.llm.provider.LlmCompletionRequest.LlmChatMessage;
import ai.nova.platform.llm.provider.LlmCompletionResult;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@Service
public class InferenceService {

    private final LLMGateway llmGateway;
    private final ModelRegistryService modelRegistryService;
    private final ContextService contextService;
    private final LlmConversationService conversationService;
    private final ConcurrentHashMap<String, AtomicBoolean> cancelTokens = new ConcurrentHashMap<>();

    public InferenceService(
            LLMGateway llmGateway,
            ModelRegistryService modelRegistryService,
            ContextService contextService,
            LlmConversationService conversationService) {
        this.llmGateway = llmGateway;
        this.modelRegistryService = modelRegistryService;
        this.contextService = contextService;
        this.conversationService = conversationService;
    }

    @Transactional
    public CompletionResponse chatCompletion(ChatCompletionRequest request, AuthenticatedUser user) {
        String cancelToken = UUID.randomUUID().toString();
        AtomicBoolean cancelled = new AtomicBoolean(false);
        cancelTokens.put(cancelToken, cancelled);
        try {
            checkCancelled(cancelled);
            LlmModelEntity model = resolveModel(user.getOrganizationId(), request.modelId(), request.modelCode());
            ensureReady(model);
            List<LlmChatMessage> messages = contextService.buildMessages(request, user);
            checkCancelled(cancelled);
            LlmCompletionResult result = llmGateway.complete(
                    user.getOrganizationId(),
                    model,
                    new LlmCompletionRequest(
                            model.getCode(), messages, request.maxTokens(), request.temperature(), false));
            UUID conversationId = request.conversationId();
            if (conversationId != null) {
                conversationService.appendAssistantReply(conversationId, user, result);
            }
            return new CompletionResponse(
                    result.content(),
                    result.inputTokens(),
                    result.outputTokens(),
                    result.latencyMs(),
                    result.providerType(),
                    result.finishReason(),
                    conversationId,
                    cancelToken);
        } finally {
            cancelTokens.remove(cancelToken);
        }
    }

    @Transactional
    public CompletionResponse textCompletion(TextCompletionRequest request, AuthenticatedUser user) {
        ChatCompletionRequest chat = new ChatCompletionRequest(
                request.modelCode(),
                request.modelId(),
                null,
                List.of(new ChatMessageDto("user", request.prompt())),
                request.maxTokens(),
                request.temperature(),
                null,
                null);
        return chatCompletion(chat, user);
    }

    @Transactional
    public List<CompletionResponse> batch(BatchCompletionRequest request, AuthenticatedUser user) {
        List<CompletionResponse> responses = new ArrayList<>();
        if (request.requests() == null) {
            return responses;
        }
        for (ChatCompletionRequest item : request.requests()) {
            responses.add(chatCompletion(item, user));
        }
        return responses;
    }

    public boolean cancel(String cancelToken) {
        AtomicBoolean flag = cancelTokens.get(cancelToken);
        if (flag == null) {
            return false;
        }
        flag.set(true);
        return true;
    }

    public LlmCompletionResult completeInternal(LlmModelEntity model, LlmCompletionRequest request, UUID organizationId) {
        return llmGateway.complete(organizationId, model, request);
    }

    private LlmModelEntity resolveModel(UUID organizationId, UUID modelId, String modelCode) {
        if (modelId != null) {
            return modelRegistryService.requireEntity(organizationId, modelId);
        }
        if (modelCode != null && !modelCode.isBlank()) {
            return modelRegistryService.requireByCode(organizationId, modelCode);
        }
        return modelRegistryService.requireByCode(organizationId, "deterministic-chat-v1");
    }

    private static void ensureReady(LlmModelEntity model) {
        if (!model.isEnabled()) {
            throw new ApiException(HttpStatus.CONFLICT, LlmErrorCodes.INVALID_STATE, "Model is disabled");
        }
        if (model.getStatus() == LlmModelStatus.DISABLED || model.getStatus() == LlmModelStatus.ERROR) {
            throw new ApiException(
                    HttpStatus.CONFLICT, LlmErrorCodes.INVALID_STATE, "Model is not ready: " + model.getStatus());
        }
    }

    private static void checkCancelled(AtomicBoolean cancelled) {
        if (cancelled.get()) {
            throw new ApiException(HttpStatus.CONFLICT, LlmErrorCodes.CANCELLED, "Inference cancelled");
        }
    }
}
