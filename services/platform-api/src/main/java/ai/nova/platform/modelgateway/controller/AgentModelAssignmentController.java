package ai.nova.platform.modelgateway.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.AgentModelAssignmentResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.AssignAgentModelRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateAgentModelAssignmentRequest;
import ai.nova.platform.modelgateway.service.AgentModelAssignmentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/agents/{agentId}/models")
public class AgentModelAssignmentController {

    private final AgentModelAssignmentService assignmentService;

    public AgentModelAssignmentController(AgentModelAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<AgentModelAssignmentResponse> list(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return assignmentService.list(projectId, agentId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentModelAssignmentResponse assign(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @Valid @RequestBody AssignAgentModelRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return assignmentService.assign(projectId, agentId, request, user);
    }

    @PutMapping("/{assignmentId}")
    public AgentModelAssignmentResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @PathVariable UUID assignmentId,
            @Valid @RequestBody UpdateAgentModelAssignmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return assignmentService.update(projectId, agentId, assignmentId, request, user);
    }

    @DeleteMapping("/{assignmentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void remove(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @PathVariable UUID assignmentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        assignmentService.remove(projectId, agentId, assignmentId, user);
    }
}
