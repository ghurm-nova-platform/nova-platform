package ai.nova.platform.modelgateway.mapper;

import org.springframework.stereotype.Component;

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.AgentModelAssignmentResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ModelResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ModelUsageDailyResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProjectModelResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProviderResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.RoutingPolicyResponse;
import ai.nova.platform.modelgateway.entity.AgentModelAssignment;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.ModelRoutingPolicy;
import ai.nova.platform.modelgateway.entity.ModelUsageDaily;
import ai.nova.platform.modelgateway.entity.ProjectModel;

@Component
public class ModelGatewayMapper {

    public ProviderResponse toProviderResponse(AiProvider provider) {
        return new ProviderResponse(
                provider.getId(),
                provider.getProviderKey(),
                provider.getName(),
                provider.getDescription(),
                provider.getProviderType(),
                provider.getAdapterKey(),
                provider.getCredentialReference(),
                provider.getRegion(),
                provider.getEndpointProfile(),
                provider.getAzureResourceName(),
                provider.getAzureApiVersion(),
                provider.getLastConnectionTestStatus() != null
                        ? provider.getLastConnectionTestStatus()
                        : ai.nova.platform.modelgateway.entity.ConnectionTestStatus.NEVER,
                provider.getLastConnectionTestAt(),
                provider.getLastConnectionTestErrorCode(),
                provider.getLastModelSyncAt(),
                provider.getLastModelSyncStatus(),
                provider.getLastModelSyncErrorCode(),
                provider.getLastModelSyncDiscoveredCount(),
                provider.getLastModelSyncCreatedCount(),
                provider.getLastModelSyncUpdatedCount(),
                provider.getLastModelSyncUnchangedCount(),
                provider.getStatus(),
                provider.getRequestTimeoutSeconds(),
                provider.getMaxConcurrentRequests(),
                provider.getMaxRetries(),
                provider.getRetryBackoffMs(),
                provider.getVersion(),
                provider.getCreatedAt(),
                provider.getUpdatedAt());
    }

    public ModelResponse toModelResponse(AiModel model) {
        return new ModelResponse(
                model.getId(),
                model.getProviderId(),
                model.getModelKey(),
                model.getProviderModelId(),
                model.getDisplayName(),
                model.getDescription(),
                model.getModelType(),
                model.getStatus(),
                model.getContextWindowTokens(),
                model.getMaxOutputTokens(),
                model.isSupportsTools(),
                model.isSupportsKnowledgeContext(),
                model.isSupportsJsonOutput(),
                model.isSupportsStreaming(),
                model.isSupportsSystemMessages(),
                model.getInputCostPerMillion(),
                model.getOutputCostPerMillion(),
                model.getCurrencyCode(),
                model.getVersion(),
                model.getCreatedAt(),
                model.getUpdatedAt());
    }

    public ProjectModelResponse toProjectModelResponse(ProjectModel projectModel, AiModel model, AiProvider provider) {
        return new ProjectModelResponse(
                projectModel.getId(),
                projectModel.getModelId(),
                model.getModelKey(),
                model.getDisplayName(),
                provider.getId(),
                provider.getName(),
                projectModel.isEnabled(),
                projectModel.isDefault(),
                projectModel.getMaximumInputTokensOverride(),
                projectModel.getMaximumOutputTokensOverride(),
                projectModel.getDailyRequestLimit(),
                projectModel.getMonthlyRequestLimit(),
                projectModel.getVersion(),
                projectModel.getCreatedAt(),
                projectModel.getUpdatedAt());
    }

    public AgentModelAssignmentResponse toAssignmentResponse(
            AgentModelAssignment assignment, AiModel model, AiProvider provider) {
        return new AgentModelAssignmentResponse(
                assignment.getId(),
                assignment.getModelId(),
                model.getModelKey(),
                model.getDisplayName(),
                provider.getId(),
                provider.getName(),
                assignment.getAssignmentRole(),
                assignment.getPriority(),
                assignment.isEnabled(),
                assignment.getTemperatureOverride(),
                assignment.getMaximumOutputTokensOverride(),
                assignment.getVersion(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    public RoutingPolicyResponse toRoutingPolicyResponse(ModelRoutingPolicy policy) {
        return new RoutingPolicyResponse(
                policy.getId(),
                policy.getProjectId(),
                policy.getAgentId(),
                policy.getPolicyKey(),
                policy.getName(),
                policy.getDescription(),
                policy.getStatus(),
                policy.getStrategy(),
                policy.isFallbackEnabled(),
                policy.isRetryEnabled(),
                policy.getMaximumProviderAttempts(),
                policy.getMaximumTotalDurationMs(),
                policy.isRequireToolSupport(),
                policy.isRequireKnowledgeSupport(),
                policy.getVersion(),
                policy.getCreatedAt(),
                policy.getUpdatedAt());
    }

    public ModelUsageDailyResponse toUsageResponse(ModelUsageDaily usage) {
        return new ModelUsageDailyResponse(
                usage.getProviderId(),
                usage.getModelId(),
                usage.getUsageDate(),
                usage.getRequestCount(),
                usage.getSuccessfulRequestCount(),
                usage.getFailedRequestCount(),
                usage.getInputTokens(),
                usage.getOutputTokens(),
                usage.getEstimatedCost(),
                usage.getCurrencyCode());
    }
}
