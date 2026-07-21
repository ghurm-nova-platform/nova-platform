package ai.nova.platform.llm.mapper;

import org.springframework.stereotype.Component;

import ai.nova.platform.llm.dto.LlmDtos.ConfigEntryView;
import ai.nova.platform.llm.dto.LlmDtos.ConversationView;
import ai.nova.platform.llm.dto.LlmDtos.MessageView;
import ai.nova.platform.llm.dto.LlmDtos.ModelView;
import ai.nova.platform.llm.dto.LlmDtos.PromptView;
import ai.nova.platform.llm.dto.LlmDtos.ProviderStatusView;
import ai.nova.platform.llm.entity.LlmConversationEntity;
import ai.nova.platform.llm.entity.LlmMessageEntity;
import ai.nova.platform.llm.entity.LlmModelEntity;
import ai.nova.platform.llm.entity.LlmPromptTemplateEntity;
import ai.nova.platform.llm.entity.LlmProviderStatusEntity;
import ai.nova.platform.llm.entity.LlmRuntimeConfigEntity;

@Component
public class LlmMapper {

    public ModelView toModelView(LlmModelEntity entity) {
        return new ModelView(
                entity.getId(),
                entity.getOrganizationId(),
                entity.getCode(),
                entity.getDisplayName(),
                entity.getFamily(),
                entity.getProviderType(),
                entity.getStatus(),
                entity.isEnabled(),
                entity.getContextLength(),
                entity.getEndpointUrl(),
                entity.getOwner(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public PromptView toPromptView(LlmPromptTemplateEntity entity) {
        return new PromptView(
                entity.getId(),
                entity.getCode(),
                entity.getName(),
                entity.getCategory(),
                entity.getSystemPrompt(),
                entity.getUserPromptTemplate(),
                entity.getAssistantPromptTemplate(),
                entity.getVariablesJson(),
                entity.getTemplateVersion(),
                entity.isEnabled(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public ConversationView toConversationView(LlmConversationEntity entity) {
        return new ConversationView(
                entity.getId(),
                entity.getModelId(),
                entity.getProjectId(),
                entity.getTitle(),
                entity.getStatus(),
                entity.getSummary(),
                entity.getTokenUsageInput(),
                entity.getTokenUsageOutput(),
                entity.getCreatedAt(),
                entity.getUpdatedAt());
    }

    public MessageView toMessageView(LlmMessageEntity entity) {
        return new MessageView(
                entity.getId(),
                entity.getRole(),
                entity.getContent(),
                entity.getTokenCount(),
                entity.getSequenceNo(),
                entity.getCreatedAt());
    }

    public ProviderStatusView toProviderStatusView(LlmProviderStatusEntity entity) {
        return new ProviderStatusView(
                entity.getProviderType(),
                entity.getStatus(),
                entity.getEndpointUrl(),
                entity.getLastHealthCheckAt(),
                entity.getLastError());
    }

    public ConfigEntryView toConfigEntryView(LlmRuntimeConfigEntity entity) {
        return new ConfigEntryView(entity.getConfigKey(), entity.getConfigValue(), entity.getUpdatedAt());
    }
}
