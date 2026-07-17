package ai.nova.platform.execution.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.execution.dto.ExecutionDtos.ExecuteRequest;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecuteResponse;
import ai.nova.platform.execution.service.ExecutionService;
import ai.nova.platform.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

@RestController
@RequestMapping("/api/projects/{projectId}/agents/{agentId}/execute")
public class AgentExecuteController {

    private final ExecutionService executionService;

    public AgentExecuteController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @PostMapping
    public ExecuteResponse execute(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @Valid @RequestBody ExecuteRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return executionService.execute(projectId, agentId, request, user);
    }
}
