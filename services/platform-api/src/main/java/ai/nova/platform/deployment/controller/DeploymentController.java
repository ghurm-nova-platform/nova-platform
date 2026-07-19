package ai.nova.platform.deployment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.deployment.dto.DeploymentDtos.Deployment;
import ai.nova.platform.deployment.dto.DeploymentDtos.EnvironmentView;
import ai.nova.platform.deployment.dto.DeploymentDtos.ObserveDeploymentRequest;
import ai.nova.platform.deployment.service.DeploymentObservationService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/deployments")
@Validated
public class DeploymentController {

    private final DeploymentObservationService observationService;

    public DeploymentController(DeploymentObservationService observationService) {
        this.observationService = observationService;
    }

    @PostMapping("/observe")
    public Deployment observe(
            @Valid @RequestBody ObserveDeploymentRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return observationService.observe(request, user);
    }

    @PostMapping("/{id}/verify")
    public Deployment verify(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return observationService.verify(id, user);
    }

    @GetMapping
    public List<Deployment> list(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return observationService.list(projectId, user);
    }

    @GetMapping("/environments")
    public List<EnvironmentView> environments(@AuthenticationPrincipal AuthenticatedUser user) {
        return observationService.listEnvironments(user);
    }

    @GetMapping("/{id}")
    public Deployment get(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return observationService.get(id, user);
    }

    @GetMapping("/{id}/history")
    public Deployment history(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return observationService.history(id, user);
    }
}
