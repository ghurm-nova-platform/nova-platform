package ai.nova.platform.knowledge.controller;

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

import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeAssignRequest;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeAssignmentResponse;
import ai.nova.platform.knowledge.dto.KnowledgeDtos.AgentKnowledgeUpdateRequest;
import ai.nova.platform.knowledge.service.AgentKnowledgeAssignmentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/agents/{agentId}/knowledge-bases")
public class AgentKnowledgeController {

    private final AgentKnowledgeAssignmentService assignmentService;

    public AgentKnowledgeController(AgentKnowledgeAssignmentService assignmentService) {
        this.assignmentService = assignmentService;
    }

    @GetMapping
    public List<AgentKnowledgeAssignmentResponse> list(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return assignmentService.list(projectId, agentId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentKnowledgeAssignmentResponse assign(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @Valid @RequestBody AgentKnowledgeAssignRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return assignmentService.assign(projectId, agentId, request, user);
    }

    @PutMapping("/{knowledgeBaseId}")
    public AgentKnowledgeAssignmentResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @PathVariable UUID knowledgeBaseId,
            @Valid @RequestBody AgentKnowledgeUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return assignmentService.update(projectId, agentId, knowledgeBaseId, request, user);
    }

    @DeleteMapping("/{knowledgeBaseId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void unassign(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @PathVariable UUID knowledgeBaseId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        assignmentService.unassign(projectId, agentId, knowledgeBaseId, user);
    }
}
