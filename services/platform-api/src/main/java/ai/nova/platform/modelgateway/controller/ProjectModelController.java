package ai.nova.platform.modelgateway.controller;

import java.util.List;
import java.util.UUID;

import jakarta.validation.Valid;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.AssignProjectModelRequest;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.ProjectModelResponse;
import ai.nova.platform.modelgateway.dto.ModelGatewayDtos.UpdateProjectModelRequest;
import ai.nova.platform.modelgateway.service.ProjectModelService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/projects/{projectId}/models")
public class ProjectModelController {

    private final ProjectModelService projectModelService;

    public ProjectModelController(ProjectModelService projectModelService) {
        this.projectModelService = projectModelService;
    }

    @GetMapping
    public List<ProjectModelResponse> list(
            @PathVariable UUID projectId, @AuthenticationPrincipal AuthenticatedUser user) {
        return projectModelService.list(projectId, user);
    }

    @PostMapping
    public ProjectModelResponse assign(
            @PathVariable UUID projectId,
            @Valid @RequestBody AssignProjectModelRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return projectModelService.assign(projectId, request, user);
    }

    @PutMapping("/{id}")
    public ProjectModelResponse update(
            @PathVariable UUID projectId,
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProjectModelRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return projectModelService.update(projectId, id, request, user);
    }
}
