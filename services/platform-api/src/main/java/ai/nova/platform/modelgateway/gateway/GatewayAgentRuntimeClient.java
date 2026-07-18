package ai.nova.platform.modelgateway.gateway;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.execution.repository.AgentExecutionRepository;

/**
 * Maps ExecutionRequest to AiModelGateway when the model gateway is enabled.
 * ToolCallingOrchestrator continues to depend on AgentRuntimeClient only — no direct gateway coupling.
 */
@Component
@ConditionalOnProperty(name = "nova.model-gateway.enabled", havingValue = "true")
public class GatewayAgentRuntimeClient implements AgentRuntimeClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayAgentRuntimeClient.class);

    private final AiModelGateway modelGateway;
    private final AgentExecutionRepository executionRepository;

    public GatewayAgentRuntimeClient(
            AiModelGateway modelGateway, AgentExecutionRepository executionRepository) {
        this.modelGateway = modelGateway;
        this.executionRepository = executionRepository;
    }

    @Override
    public void createOrUpdateAgentDefinition(
            UUID organizationId, UUID projectId, UUID agentId, String name, String status) {
        log.debug("Skipping external runtime sync for agent {} (gateway mode)", agentId);
    }

    @Override
    public void archiveAgentDefinition(UUID organizationId, UUID projectId, UUID agentId) {
        log.debug("Skipping external runtime archive for agent {} (gateway mode)", agentId);
    }

    @Override
    public RuntimeTurnResult execute(ExecutionRequest request) {
        boolean requiresTools = request.availableTools() != null && !request.availableTools().isEmpty();
        boolean requiresKnowledge = request.knowledgeContext() != null && !request.knowledgeContext().isEmpty();

        UUID createdBy = executionRepository
                .findById(request.executionId())
                .map(execution -> execution.getCreatedBy())
                .orElse(null);

        String modelReference = request.model() != null && !request.model().isBlank() ? request.model().trim() : null;
        ModelGatewayRequest gatewayRequest = new ModelGatewayRequest(
                request.organizationId(),
                request.projectId(),
                request.agentId(),
                request.executionId(),
                request.conversationId(),
                createdBy,
                request.systemPrompt(),
                request.messages(),
                request.availableTools(),
                request.toolResults(),
                request.knowledgeContext(),
                requiresTools,
                requiresKnowledge,
                modelReference);

        ModelGatewayResponse response = modelGateway.invoke(gatewayRequest);
        return response.turnResult();
    }

    @Override
    public void cancel(UUID executionId) {
        log.debug("Execution {} cancel handled via lifecycle service", executionId);
    }
}
