package ai.nova.platform.agent.controller;

import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.agent.dto.AgentDtos.AgentCreateRequest;
import ai.nova.platform.agent.dto.AgentDtos.AgentResponse;
import ai.nova.platform.agent.dto.AgentDtos.AgentStatusRequest;
import ai.nova.platform.agent.dto.AgentDtos.AgentUpdateRequest;
import ai.nova.platform.agent.entity.AgentStatus;
import ai.nova.platform.agent.service.AgentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/agents")
public class AgentController {

    private final AgentService agentService;

    public AgentController(AgentService agentService) {
        this.agentService = agentService;
    }

    @GetMapping
    public Page<AgentResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) AgentStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return agentService.list(projectId, user, search, status, pageable);
    }

    @GetMapping("/{agentId}")
    public AgentResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return agentService.get(projectId, agentId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AgentResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody AgentCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return agentService.create(projectId, request, user);
    }

    @PutMapping("/{agentId}")
    public AgentResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @Valid @RequestBody AgentUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return agentService.update(projectId, agentId, request, user);
    }

    @PatchMapping("/{agentId}/status")
    public AgentResponse updateStatus(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @Valid @RequestBody AgentStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return agentService.updateStatus(projectId, agentId, request, user);
    }

    @DeleteMapping("/{agentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @PathVariable UUID projectId,
            @PathVariable UUID agentId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        agentService.archive(projectId, agentId, user);
    }
}
