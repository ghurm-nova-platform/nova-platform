package ai.nova.platform.tool.mapper;

import org.springframework.stereotype.Component;

import ai.nova.platform.tool.dto.ToolDtos.AgentToolAssignmentResponse;
import ai.nova.platform.tool.dto.ToolDtos.ToolCallResponse;
import ai.nova.platform.tool.dto.ToolDtos.ToolResponse;
import ai.nova.platform.tool.entity.AgentToolAssignment;
import ai.nova.platform.tool.entity.ExecutionToolCall;
import ai.nova.platform.tool.entity.ToolDefinition;

@Component
public class ToolMapper {

    public ToolResponse toResponse(ToolDefinition tool) {
        return new ToolResponse(
                tool.getId(),
                tool.getOrganizationId(),
                tool.getProjectId(),
                tool.getToolKey(),
                tool.getName(),
                tool.getDescription(),
                tool.getToolType(),
                tool.getExecutorKey(),
                tool.getInputSchema(),
                tool.getOutputSchema(),
                tool.getStatus(),
                tool.isRequiresApproval(),
                tool.getMaxExecutionSeconds(),
                tool.getMaxOutputCharacters(),
                tool.getVersion(),
                tool.getCreatedBy(),
                tool.getUpdatedBy(),
                tool.getCreatedAt(),
                tool.getUpdatedAt());
    }

    public AgentToolAssignmentResponse toAssignmentResponse(AgentToolAssignment assignment, ToolDefinition tool) {
        return new AgentToolAssignmentResponse(
                assignment.getId(),
                assignment.getAgentId(),
                assignment.getToolId(),
                tool.getToolKey(),
                tool.getName(),
                tool.getStatus(),
                assignment.isEnabled(),
                assignment.getVersion(),
                assignment.getCreatedBy(),
                assignment.getCreatedAt(),
                assignment.getUpdatedAt());
    }

    public ToolCallResponse toToolCallResponse(ExecutionToolCall toolCall) {
        return new ToolCallResponse(
                toolCall.getId(),
                toolCall.getExecutionId(),
                toolCall.getAgentId(),
                toolCall.getToolId(),
                toolCall.getToolKey(),
                toolCall.getRuntimeCallId(),
                toolCall.getSequenceNumber(),
                toolCall.getStatus(),
                toolCall.getInputPayload(),
                toolCall.getOutputPayload(),
                toolCall.getErrorCode(),
                toolCall.getRequestedAt(),
                toolCall.getStartedAt(),
                toolCall.getCompletedAt(),
                toolCall.getDurationMs(),
                toolCall.getApprovedBy(),
                toolCall.getApprovedAt(),
                toolCall.getCreatedBy());
    }
}
