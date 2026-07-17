package ai.nova.platform.agent.mapper;

import org.springframework.stereotype.Component;

import ai.nova.platform.agent.dto.AgentDtos.AgentResponse;
import ai.nova.platform.agent.entity.Agent;

@Component
public class AgentMapper {

    public AgentResponse toResponse(Agent agent) {
        return new AgentResponse(
                agent.getId(),
                agent.getOrganizationId(),
                agent.getProjectId(),
                agent.getName(),
                agent.getDescription(),
                agent.getSystemPrompt(),
                agent.getPromptId(),
                agent.getPromptVersionId(),
                agent.getModelProvider(),
                agent.getModelName(),
                agent.getTemperature(),
                agent.getMaxTokens(),
                agent.getStatus(),
                agent.getVisibility(),
                agent.getVersion(),
                agent.getCreatedBy(),
                agent.getUpdatedBy(),
                agent.getCreatedAt(),
                agent.getUpdatedAt());
    }
}
