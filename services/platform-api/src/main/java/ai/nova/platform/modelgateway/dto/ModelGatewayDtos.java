package ai.nova.platform.modelgateway.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiModelType;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.AssignmentRole;
import ai.nova.platform.modelgateway.entity.ConnectionTestStatus;
import ai.nova.platform.modelgateway.entity.EndpointProfile;
import ai.nova.platform.modelgateway.entity.ModelSyncStatus;
import ai.nova.platform.modelgateway.entity.RoutingPolicyStatus;
import ai.nova.platform.modelgateway.entity.RoutingStrategy;

public final class ModelGatewayDtos {

    private ModelGatewayDtos() {
    }

    public record ProviderAdapterResponse(String adapterKey, boolean tools, boolean knowledgeContext, boolean jsonOutput, boolean systemMessages, boolean streaming) {
    }

    public record ProviderAdaptersResponse(List<ProviderAdapterResponse> adapters) {
    }

    public record CreateProviderRequest(
            @NotBlank String providerKey,
            @NotBlank String name,
            String description,
            @NotNull AiProviderType providerType,
            @NotBlank String adapterKey,
            String credentialReference,
            String region,
            EndpointProfile endpointProfile,
            String azureResourceName,
            String azureApiVersion,
            Integer requestTimeoutSeconds,
            Integer maxConcurrentRequests,
            Integer maxRetries,
            Integer retryBackoffMs) {
    }

    public record UpdateProviderRequest(
            @NotBlank String name,
            String description,
            String credentialReference,
            String region,
            EndpointProfile endpointProfile,
            String azureResourceName,
            String azureApiVersion,
            Integer requestTimeoutSeconds,
            Integer maxConcurrentRequests,
            Integer maxRetries,
            Integer retryBackoffMs,
            @NotNull Integer version) {
    }

    public record ProviderResponse(
            UUID id,
            String providerKey,
            String name,
            String description,
            AiProviderType providerType,
            String adapterKey,
            String credentialReference,
            String region,
            EndpointProfile endpointProfile,
            String azureResourceName,
            String azureApiVersion,
            ConnectionTestStatus lastConnectionTestStatus,
            Instant lastConnectionTestAt,
            String lastConnectionTestErrorCode,
            Instant lastModelSyncAt,
            ModelSyncStatus lastModelSyncStatus,
            String lastModelSyncErrorCode,
            Integer lastModelSyncDiscoveredCount,
            Integer lastModelSyncCreatedCount,
            Integer lastModelSyncUpdatedCount,
            Integer lastModelSyncUnchangedCount,
            AiProviderStatus status,
            Integer requestTimeoutSeconds,
            Integer maxConcurrentRequests,
            Integer maxRetries,
            Integer retryBackoffMs,
            Integer version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ConnectionTestResponse(
            ConnectionTestStatus status, String errorCode, Instant testedAt) {
    }

    public record CreateModelRequest(
            @NotBlank String modelKey,
            @NotBlank String providerModelId,
            @NotBlank String displayName,
            String description,
            @NotNull AiModelType modelType,
            @NotNull Integer contextWindowTokens,
            @NotNull Integer maxOutputTokens,
            Boolean supportsTools,
            Boolean supportsKnowledgeContext,
            Boolean supportsJsonOutput,
            Boolean supportsStreaming,
            Boolean supportsSystemMessages,
            BigDecimal inputCostPerMillion,
            BigDecimal outputCostPerMillion,
            String currencyCode) {
    }

    public record UpdateModelRequest(
            @NotBlank String displayName,
            String description,
            @NotNull Integer contextWindowTokens,
            @NotNull Integer maxOutputTokens,
            Boolean supportsTools,
            Boolean supportsKnowledgeContext,
            Boolean supportsJsonOutput,
            Boolean supportsStreaming,
            Boolean supportsSystemMessages,
            BigDecimal inputCostPerMillion,
            BigDecimal outputCostPerMillion,
            String currencyCode,
            @NotNull Integer version) {
    }

    public record ModelResponse(
            UUID id,
            UUID providerId,
            String modelKey,
            String providerModelId,
            String displayName,
            String description,
            AiModelType modelType,
            AiModelStatus status,
            Integer contextWindowTokens,
            Integer maxOutputTokens,
            boolean supportsTools,
            boolean supportsKnowledgeContext,
            boolean supportsJsonOutput,
            boolean supportsStreaming,
            boolean supportsSystemMessages,
            BigDecimal inputCostPerMillion,
            BigDecimal outputCostPerMillion,
            String currencyCode,
            Integer version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record AssignProjectModelRequest(@NotNull UUID modelId, Boolean isDefault) {
    }

    public record UpdateProjectModelRequest(
            Boolean enabled,
            Boolean isDefault,
            Integer maximumInputTokensOverride,
            Integer maximumOutputTokensOverride,
            Integer dailyRequestLimit,
            Integer monthlyRequestLimit,
            @NotNull Integer version) {
    }

    public record ProjectModelResponse(
            UUID id,
            UUID modelId,
            String modelKey,
            String displayName,
            UUID providerId,
            String providerName,
            boolean enabled,
            boolean isDefault,
            Integer maximumInputTokensOverride,
            Integer maximumOutputTokensOverride,
            Integer dailyRequestLimit,
            Integer monthlyRequestLimit,
            Integer version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record AssignAgentModelRequest(
            @NotNull UUID modelId,
            @NotNull AssignmentRole assignmentRole,
            @NotNull Integer priority,
            BigDecimal temperatureOverride,
            Integer maximumOutputTokensOverride) {
    }

    public record UpdateAgentModelAssignmentRequest(
            Boolean enabled,
            Integer priority,
            BigDecimal temperatureOverride,
            Integer maximumOutputTokensOverride,
            @NotNull Integer version) {
    }

    public record AgentModelAssignmentResponse(
            UUID id,
            UUID modelId,
            String modelKey,
            String displayName,
            UUID providerId,
            String providerName,
            AssignmentRole assignmentRole,
            Integer priority,
            boolean enabled,
            BigDecimal temperatureOverride,
            Integer maximumOutputTokensOverride,
            Integer version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record CreateRoutingPolicyRequest(
            @NotBlank String policyKey,
            @NotBlank String name,
            String description,
            UUID agentId,
            @NotNull RoutingStrategy strategy,
            Boolean fallbackEnabled,
            Boolean retryEnabled,
            Integer maximumProviderAttempts,
            Long maximumTotalDurationMs,
            Boolean requireToolSupport,
            Boolean requireKnowledgeSupport) {
    }

    public record UpdateRoutingPolicyRequest(
            @NotBlank String name,
            String description,
            @NotNull RoutingStrategy strategy,
            Boolean fallbackEnabled,
            Boolean retryEnabled,
            Integer maximumProviderAttempts,
            Long maximumTotalDurationMs,
            Boolean requireToolSupport,
            Boolean requireKnowledgeSupport,
            @NotNull Integer version) {
    }

    public record RoutingPolicyResponse(
            UUID id,
            UUID projectId,
            UUID agentId,
            String policyKey,
            String name,
            String description,
            RoutingPolicyStatus status,
            RoutingStrategy strategy,
            boolean fallbackEnabled,
            boolean retryEnabled,
            Integer maximumProviderAttempts,
            Long maximumTotalDurationMs,
            boolean requireToolSupport,
            boolean requireKnowledgeSupport,
            Integer version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ModelUsageDailyResponse(
            UUID providerId,
            UUID modelId,
            LocalDate usageDate,
            long requestCount,
            long successfulRequestCount,
            long failedRequestCount,
            long inputTokens,
            long outputTokens,
            BigDecimal estimatedCost,
            String currencyCode) {
    }

    public record ModelUsageResponse(List<ModelUsageDailyResponse> entries) {
    }
}
