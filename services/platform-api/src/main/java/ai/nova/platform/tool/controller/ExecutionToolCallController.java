package ai.nova.platform.tool.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.execution.dto.ExecutionDtos.ExecuteResponse;
import ai.nova.platform.execution.service.ExecutionService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.tool.dto.ToolDtos.ToolCallApproveRequest;
import ai.nova.platform.tool.dto.ToolDtos.ToolCallRejectRequest;
import ai.nova.platform.tool.dto.ToolDtos.ToolCallResponse;
import ai.nova.platform.tool.service.ToolCallService;

@RestController
@RequestMapping("/api/projects/{projectId}/executions")
public class ExecutionToolCallController {

    private final ToolCallService toolCallService;
    private final ExecutionService executionService;

    public ExecutionToolCallController(ToolCallService toolCallService, ExecutionService executionService) {
        this.toolCallService = toolCallService;
        this.executionService = executionService;
    }

    @GetMapping("/{executionId}/tool-calls")
    public List<ToolCallResponse> list(
            @PathVariable UUID projectId,
            @PathVariable UUID executionId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toolCallService.list(projectId, executionId, user);
    }

    @GetMapping("/{executionId}/tool-calls/{toolCallId}")
    public ToolCallResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID executionId,
            @PathVariable UUID toolCallId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toolCallService.get(projectId, executionId, toolCallId, user);
    }

    @PostMapping("/{executionId}/tool-calls/{toolCallId}/approve")
    public ToolCallResponse approve(
            @PathVariable UUID projectId,
            @PathVariable UUID executionId,
            @PathVariable UUID toolCallId,
            @Valid @RequestBody ToolCallApproveRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toolCallService.approve(projectId, executionId, toolCallId, request, user);
    }

    @PostMapping("/{executionId}/tool-calls/{toolCallId}/reject")
    public ToolCallResponse reject(
            @PathVariable UUID projectId,
            @PathVariable UUID executionId,
            @PathVariable UUID toolCallId,
            @Valid @RequestBody ToolCallRejectRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toolCallService.reject(projectId, executionId, toolCallId, request, user);
    }

    @PostMapping("/{executionId}/continue")
    public ExecuteResponse continueExecution(
            @PathVariable UUID projectId,
            @PathVariable UUID executionId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return executionService.continueAfterToolApproval(projectId, executionId, user);
    }
}
