package ai.nova.platform.execution.controller;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.execution.dto.ExecutionDtos.ExecutionDetailResponse;
import ai.nova.platform.execution.dto.ExecutionDtos.ExecutionSummaryResponse;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.service.ExecutionService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/executions")
public class ExecutionController {

    private final ExecutionService executionService;

    public ExecutionController(ExecutionService executionService) {
        this.executionService = executionService;
    }

    @GetMapping
    public Page<ExecutionSummaryResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) UUID agentId,
            @RequestParam(required = false) ExecutionStatus status,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return executionService.list(projectId, user, agentId, status, pageable);
    }

    @GetMapping("/{executionId}")
    public ExecutionDetailResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID executionId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return executionService.get(projectId, executionId, user);
    }

    @PostMapping("/{executionId}/cancel")
    public ExecutionDetailResponse cancel(
            @PathVariable UUID projectId,
            @PathVariable UUID executionId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return executionService.cancel(projectId, executionId, user);
    }
}
