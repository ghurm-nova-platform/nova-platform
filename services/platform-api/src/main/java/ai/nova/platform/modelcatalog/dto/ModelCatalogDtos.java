package ai.nova.platform.modelcatalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelgateway.entity.AiModelSource;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiModelType;
import ai.nova.platform.modelgateway.entity.ModelSyncStatus;

public final class ModelCatalogDtos {

    private ModelCatalogDtos() {
    }

    public record CreateCatalogModelRequest(
            @NotNull UUID providerId,
            @NotBlank String modelKey,
            @NotBlank String providerModelId,
            @NotBlank String displayName,
            String description,
            @NotNull AiModelType modelType,
            String modelFamily,
            String modelVersion,
            @NotNull Integer contextWindowTokens,
            Integer maxInputTokens,
            @NotNull Integer maxOutputTokens,
            BigDecimal defaultTemperature,
            BigDecimal defaultTopP,
            Integer defaultMaxOutputTokens,
            Boolean supportsKnowledgeContext,
            Boolean supportsSystemMessages,
            BigDecimal inputCostPerMillion,
            BigDecimal outputCostPerMillion,
            String currency,
            List<AiModelCapability> capabilities) {
    }

    public record UpdateCatalogModelRequest(
            @NotBlank String displayName,
            String description,
            String modelFamily,
            String modelVersion,
            @NotNull Integer contextWindowTokens,
            Integer maxInputTokens,
            @NotNull Integer maxOutputTokens,
            BigDecimal defaultTemperature,
            BigDecimal defaultTopP,
            Integer defaultMaxOutputTokens,
            Boolean supportsKnowledgeContext,
            Boolean supportsSystemMessages,
            BigDecimal inputCostPerMillion,
            BigDecimal outputCostPerMillion,
            String currency,
            @NotNull Integer version) {
    }

    public record ReplaceCapabilitiesRequest(@NotNull List<CapabilityInput> capabilities) {
    }

    public record CapabilityInput(
            @NotNull AiModelCapability capability, Boolean enabled, String metadataJson) {
    }

    public record CreateAliasRequest(@NotBlank String alias) {
    }

    public record CapabilityResponse(
            AiModelCapability capability, boolean enabled, String metadataJson, Instant createdAt) {
    }

    public record AliasResponse(
            UUID id,
            UUID modelId,
            String alias,
            String normalizedAlias,
            Instant createdAt) {
    }

    public record CatalogModelResponse(
            UUID id,
            UUID providerId,
            String providerName,
            String modelKey,
            String providerModelId,
            String displayName,
            String description,
            AiModelType modelType,
            AiModelStatus status,
            AiModelSource source,
            String modelFamily,
            String modelVersion,
            Integer contextWindowTokens,
            Integer contextWindow,
            Integer maxInputTokens,
            Integer maxOutputTokens,
            BigDecimal defaultTemperature,
            BigDecimal defaultTopP,
            Integer defaultMaxOutputTokens,
            boolean supportsTools,
            boolean supportsKnowledgeContext,
            boolean supportsJsonOutput,
            boolean supportsStreaming,
            boolean supportsSystemMessages,
            BigDecimal inputCostPerMillion,
            BigDecimal outputCostPerMillion,
            String currency,
            Instant discoveredAt,
            Instant lastSyncedAt,
            Instant lastSeenAt,
            List<CapabilityResponse> capabilities,
            Integer version,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record SyncResultResponse(
            UUID providerId,
            int discoveredCount,
            int createdCount,
            int updatedCount,
            int unchangedCount,
            boolean stale,
            ModelSyncStatus status,
            String errorCode,
            Instant syncedAt) {
    }
}
