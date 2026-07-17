package ai.nova.platform.execution.mapper;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ai.nova.platform.execution.dto.ExecutionDtos.ExecutionDetailResponse;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecutionMessageResponse;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecutionSummaryResponse;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecuteResponse;
import ai.nova.platform.execution.dto.ExecutionDtos.TokenUsage;
import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionMessage;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.KnowledgeCitationResponse;

@Component
public class ExecutionMapper {

    public ExecutionSummaryResponse toSummary(AgentExecution execution) {
        return new ExecutionSummaryResponse(
                execution.getId(),
                execution.getAgentId(),
                execution.getPromptVersionId(),
                execution.getConversationId(),
                execution.getProvider(),
                execution.getModel(),
                execution.getStatus(),
                execution.getInputTokens(),
                execution.getOutputTokens(),
                execution.getTotalTokens(),
                execution.getLatencyMs(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getCreatedBy(),
                execution.getCreatedAt(),
                execution.getErrorMessage());
    }

    public ExecutionDetailResponse toDetail(AgentExecution execution, List<ExecutionMessage> messages) {
        List<ExecutionMessageResponse> messageResponses = messages.stream()
                .map(m -> new ExecutionMessageResponse(m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return new ExecutionDetailResponse(
                execution.getId(),
                execution.getAgentId(),
                execution.getPromptVersionId(),
                execution.getConversationId(),
                execution.getProvider(),
                execution.getModel(),
                execution.getStatus(),
                execution.getInputTokens(),
                execution.getOutputTokens(),
                execution.getTotalTokens(),
                execution.getLatencyMs(),
                execution.getStartedAt(),
                execution.getCompletedAt(),
                execution.getCreatedBy(),
                execution.getCreatedAt(),
                execution.getErrorMessage(),
                messageResponses);
    }

    public ExecuteResponse toExecuteResponse(
            AgentExecution execution, String response, String renderedPrompt) {
        return toExecuteResponse(execution, response, renderedPrompt, false, null, List.of());
    }

    public ExecuteResponse toExecuteResponse(
            AgentExecution execution,
            String response,
            String renderedPrompt,
            List<KnowledgeCitationResponse> citations) {
        return toExecuteResponse(execution, response, renderedPrompt, false, null, citations);
    }

    public ExecuteResponse toExecuteResponse(
            AgentExecution execution,
            String response,
            String renderedPrompt,
            boolean awaitingApproval,
            UUID pendingToolCallId) {
        return toExecuteResponse(
                execution, response, renderedPrompt, awaitingApproval, pendingToolCallId, List.of());
    }

    public ExecuteResponse toExecuteResponse(
            AgentExecution execution,
            String response,
            String renderedPrompt,
            boolean awaitingApproval,
            UUID pendingToolCallId,
            List<KnowledgeCitationResponse> citations) {
        TokenUsage tokens = execution.getTotalTokens() != null
                ? new TokenUsage(
                        execution.getInputTokens() != null ? execution.getInputTokens() : 0,
                        execution.getOutputTokens() != null ? execution.getOutputTokens() : 0,
                        execution.getTotalTokens())
                : new TokenUsage(0, 0, 0);
        return new ExecuteResponse(
                execution.getId(),
                execution.getStatus(),
                response,
                execution.getLatencyMs() != null ? execution.getLatencyMs() : 0L,
                tokens,
                renderedPrompt,
                execution.getErrorMessage(),
                awaitingApproval ? Boolean.TRUE : null,
                pendingToolCallId,
                citations == null ? List.of() : citations);
    }
}
