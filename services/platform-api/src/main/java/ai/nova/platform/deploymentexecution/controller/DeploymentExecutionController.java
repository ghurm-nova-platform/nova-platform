package ai.nova.platform.deploymentexecution.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.CreateExecutionRequest;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.DeploymentExecution;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.LogEntry;
import ai.nova.platform.deploymentexecution.service.DeploymentExecutionService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/deployment-executions")
@Validated
public class DeploymentExecutionController {

    private final DeploymentExecutionService deploymentExecutionService;

    public DeploymentExecutionController(DeploymentExecutionService deploymentExecutionService) {
        this.deploymentExecutionService = deploymentExecutionService;
    }

    @PostMapping({"/create", ""})
    public DeploymentExecution create(
            @Valid @RequestBody CreateExecutionRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return deploymentExecutionService.create(request, user);
    }

    @PostMapping("/{id}/start")
    public ResponseEntity<DeploymentExecution> start(
            @PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        DeploymentExecution started = deploymentExecutionService.start(id, user);
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(started);
    }

    @PostMapping("/{id}/cancel")
    public DeploymentExecution cancel(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return deploymentExecutionService.cancel(id, user);
    }

    @GetMapping
    public List<DeploymentExecution> list(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return deploymentExecutionService.list(projectId, user);
    }

    @GetMapping("/{id}")
    public DeploymentExecution get(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return deploymentExecutionService.get(id, user);
    }

    @GetMapping("/{id}/history")
    public DeploymentExecution history(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return deploymentExecutionService.history(id, user);
    }

    @GetMapping("/{id}/logs")
    public List<LogEntry> logs(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return deploymentExecutionService.logs(id, user);
    }
}
