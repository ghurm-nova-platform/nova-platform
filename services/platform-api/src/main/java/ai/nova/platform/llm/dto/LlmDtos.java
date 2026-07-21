package ai.nova.platform.llm.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import ai.nova.platform.llm.entity.LlmConversationStatus;
import ai.nova.platform.llm.entity.LlmMessageRole;
import ai.nova.platform.llm.entity.LlmModelFamily;
import ai.nova.platform.llm.entity.LlmModelStatus;
import ai.nova.platform.llm.entity.LlmPromptCategory;
import ai.nova.platform.llm.entity.LlmProviderHealthStatus;
import ai.nova.platform.llm.entity.LlmProviderType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class LlmDtos {

    private LlmDtos() {
    }

    public record ConfigResponse(
            boolean enabled,
            String defaultProvider,
            boolean fallbackToDeterministic,
            int timeoutSeconds,
            boolean ollamaEnabled,
            boolean llamacppEnabled,
            boolean vllmEnabled) {
    }

    public record MetricsSummaryResponse(Map<String, Object> metrics) {
    }

    public record HealthResponse(List<ProviderStatusView> providers) {
    }

    public record ProviderStatusView(
            LlmProviderType providerType,
            LlmProviderHealthStatus status,
            String endpointUrl,
            Instant lastHealthCheckAt,
            String lastError) {
    }

    public record RegisterModelRequest(
            @NotBlank String code,
            @NotBlank String displayName,
            @NotNull LlmModelFamily family,
            @NotNull LlmProviderType providerType,
            Integer contextLength,
            String endpointUrl,
            String owner,
            String capabilitiesJson,
            String tagsJson) {
    }

    public record UpdateModelRequest(
            String displayName,
            LlmModelFamily family,
            Integer contextLength,
            String endpointUrl,
            String owner,
            String capabilitiesJson,
            String tagsJson) {
    }

    public record ModelView(
            UUID id,
            UUID organizationId,
            String code,
            String displayName,
            LlmModelFamily family,
            LlmProviderType providerType,
            LlmModelStatus status,
            boolean enabled,
            int contextLength,
            String endpointUrl,
            String owner,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ChatMessageDto(@NotBlank String role, @NotBlank String content) {
    }

    public record ChatCompletionRequest(
            String modelCode,
            UUID modelId,
            UUID conversationId,
            List<ChatMessageDto> messages,
            Integer maxTokens,
            Double temperature,
            String context,
            List<UUID> knowledgeDocumentIds) {
    }

    public record TextCompletionRequest(
            String modelCode,
            UUID modelId,
            @NotBlank String prompt,
            Integer maxTokens,
            Double temperature) {
    }

    public record BatchCompletionRequest(List<ChatCompletionRequest> requests) {
    }

    public record CompletionResponse(
            String content,
            int inputTokens,
            int outputTokens,
            long latencyMs,
            LlmProviderType providerType,
            String finishReason,
            UUID conversationId,
            String cancelToken) {
    }

    public record CreatePromptRequest(
            @NotBlank String code,
            @NotBlank String name,
            @NotNull LlmPromptCategory category,
            String systemPrompt,
            @NotBlank String userPromptTemplate,
            String assistantPromptTemplate,
            String variablesJson) {
    }

    public record UpdatePromptRequest(
            String name,
            LlmPromptCategory category,
            String systemPrompt,
            String userPromptTemplate,
            String assistantPromptTemplate,
            String variablesJson,
            Boolean enabled) {
    }

    public record PromptView(
            UUID id,
            String code,
            String name,
            LlmPromptCategory category,
            String systemPrompt,
            String userPromptTemplate,
            String assistantPromptTemplate,
            String variablesJson,
            int templateVersion,
            boolean enabled,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record RenderPromptRequest(Map<String, String> variables) {
    }

    public record RenderPromptResponse(String systemPrompt, String userPrompt, String assistantPrompt) {
    }

    public record CreateConversationRequest(UUID modelId, UUID projectId, String title) {
    }

    public record AppendMessageRequest(@NotNull LlmMessageRole role, @NotBlank String content) {
    }

    public record ConversationView(
            UUID id,
            UUID modelId,
            UUID projectId,
            String title,
            LlmConversationStatus status,
            String summary,
            int tokenUsageInput,
            int tokenUsageOutput,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record MessageView(
            UUID id,
            LlmMessageRole role,
            String content,
            Integer tokenCount,
            int sequenceNo,
            Instant createdAt) {
    }

    public record SetConfigRequest(@NotBlank String key, @NotBlank String value) {
    }

    public record ConfigEntryView(String key, String value, Instant updatedAt) {
    }
}
