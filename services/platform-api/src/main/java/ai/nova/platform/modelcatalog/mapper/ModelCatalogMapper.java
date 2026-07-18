package ai.nova.platform.modelcatalog.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.AliasResponse;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CapabilityResponse;
import ai.nova.platform.modelcatalog.dto.ModelCatalogDtos.CatalogModelResponse;
import ai.nova.platform.modelcatalog.entity.AiModelAlias;
import ai.nova.platform.modelcatalog.entity.AiModelCapabilityEntity;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiProvider;

@Component
public class ModelCatalogMapper {

    public CatalogModelResponse toResponse(
            AiModel model, AiProvider provider, List<AiModelCapabilityEntity> capabilities) {
        String providerName = provider != null ? provider.getName() : null;
        List<CapabilityResponse> caps = capabilities == null
                ? List.of()
                : capabilities.stream().map(this::toCapabilityResponse).toList();
        return new CatalogModelResponse(
                model.getId(),
                model.getProviderId(),
                providerName,
                model.getModelKey(),
                model.getProviderModelId(),
                model.getDisplayName(),
                model.getDescription(),
                model.getModelType(),
                model.getStatus(),
                model.getSource(),
                model.getModelFamily(),
                model.getModelVersion(),
                model.getContextWindowTokens(),
                model.getContextWindow(),
                model.getMaxInputTokens(),
                model.getMaxOutputTokens(),
                model.getDefaultTemperature(),
                model.getDefaultTopP(),
                model.getDefaultMaxOutputTokens(),
                model.isSupportsTools(),
                model.isSupportsKnowledgeContext(),
                model.isSupportsJsonOutput(),
                model.isSupportsStreaming(),
                model.isSupportsSystemMessages(),
                model.getInputCostPerMillion(),
                model.getOutputCostPerMillion(),
                model.getCurrency() != null ? model.getCurrency() : model.getCurrencyCode(),
                model.getDiscoveredAt(),
                model.getLastSyncedAt(),
                model.getLastSeenAt(),
                caps,
                model.getVersion(),
                model.getCreatedAt(),
                model.getUpdatedAt());
    }

    public CapabilityResponse toCapabilityResponse(AiModelCapabilityEntity entity) {
        return new CapabilityResponse(
                entity.getCapability(), entity.isEnabled(), entity.getMetadataJson(), entity.getCreatedAt());
    }

    public AliasResponse toAliasResponse(AiModelAlias alias) {
        return new AliasResponse(
                alias.getId(),
                alias.getModelId(),
                alias.getAlias(),
                alias.getNormalizedAlias(),
                alias.getCreatedAt());
    }
}
