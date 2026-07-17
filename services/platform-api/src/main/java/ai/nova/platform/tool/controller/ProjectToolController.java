package ai.nova.platform.tool.controller;

import java.util.List;
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
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.tool.dto.ToolDtos.ExecutorKeysResponse;
import ai.nova.platform.tool.dto.ToolDtos.ToolCreateRequest;
import ai.nova.platform.tool.dto.ToolDtos.ToolResponse;
import ai.nova.platform.tool.dto.ToolDtos.ToolUpdateRequest;
import ai.nova.platform.tool.entity.ToolStatus;
import ai.nova.platform.tool.entity.ToolType;
import ai.nova.platform.tool.executor.ToolExecutorRegistry;
import ai.nova.platform.tool.security.ToolAuthorizationService;
import ai.nova.platform.tool.service.ToolService;
import ai.nova.platform.project.ProjectRepository;
import ai.nova.platform.web.error.ApiException;
import org.springframework.http.HttpStatus;

@RestController
@RequestMapping("/api/projects/{projectId}/tools")
public class ProjectToolController {

    private final ToolService toolService;
    private final ToolExecutorRegistry executorRegistry;
    private final ToolAuthorizationService authorizationService;
    private final ProjectRepository projectRepository;

    public ProjectToolController(
            ToolService toolService,
            ToolExecutorRegistry executorRegistry,
            ToolAuthorizationService authorizationService,
            ProjectRepository projectRepository) {
        this.toolService = toolService;
        this.executorRegistry = executorRegistry;
        this.authorizationService = authorizationService;
        this.projectRepository = projectRepository;
    }

    @GetMapping
    public Page<ToolResponse> list(
            @PathVariable UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) ToolStatus status,
            @RequestParam(required = false) ToolType type,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return toolService.list(projectId, user, search, status, type, pageable);
    }

    @GetMapping("/executors")
    public ExecutorKeysResponse listExecutors(
            @PathVariable UUID projectId, @AuthenticationPrincipal AuthenticatedUser user) {
        authorizationService.require(user, ToolAuthorizationService.TOOL_READ);
        projectRepository
                .findByIdAndOrganizationId(projectId, user.getOrganizationId())
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "PROJECT_NOT_FOUND", "Project not found"));
        List<String> keys = executorRegistry.allowedKeys().stream().sorted().toList();
        return new ExecutorKeysResponse(keys);
    }

    @GetMapping("/{toolId}")
    public ToolResponse get(
            @PathVariable UUID projectId,
            @PathVariable UUID toolId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toolService.get(projectId, toolId, user);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ToolResponse create(
            @PathVariable UUID projectId,
            @Valid @RequestBody ToolCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toolService.create(projectId, request, user);
    }

    @PutMapping("/{toolId}")
    public ToolResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID toolId,
            @Valid @RequestBody ToolUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toolService.update(projectId, toolId, request, user);
    }

    @PostMapping("/{toolId}/activate")
    public ToolResponse activate(
            @PathVariable UUID projectId,
            @PathVariable UUID toolId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toolService.activate(projectId, toolId, user);
    }

    @DeleteMapping("/{toolId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(
            @PathVariable UUID projectId,
            @PathVariable UUID toolId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        toolService.archive(projectId, toolId, user);
    }
}
