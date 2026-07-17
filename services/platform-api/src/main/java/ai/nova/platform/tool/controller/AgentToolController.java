package ai.nova.platform.tool.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.tool.dto.ToolDtos.AgentToolAssignRequest;
import ai.nova.platform.tool.dto.ToolDtos.AgentToolAssignmentResponse;
import ai.nova.platform.tool.service.AgentToolAssignmentService;

@RestController
@RequestMapping("/api/projects/{projectId}/agents/{agentId}/tools")
public class AgentToolController {

    private final AgentToolAssignmentService assignmentService;

    public AgentToolController(AgentToolAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<AgentToolAssignmentResponse> list(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return assignmentService.list(projectId, agentId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentToolAssignmentResponse assign(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @Valid @RequestBody AgentToolAssignRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return assignmentService.assign(projectId, agentId, request, user);
    }

    @DeleteMapping("/{toolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @PathVariable UUID toolId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        assignmentService.unassign(projectId, agentId, toolId, user);
    }
}
