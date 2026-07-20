package ai.nova.platform.environment.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import ai.nova.platform.environment.dto.EnvironmentDtos.CreateEnvironmentRequest;
import ai.nova.platform.environment.dto.EnvironmentDtos.Environment;
import ai.nova.platform.environment.dto.EnvironmentDtos.UpdateEnvironmentRequest;
import ai.nova.platform.environment.service.EnvironmentService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/environments")
@Validated
public class EnvironmentController {

    private final EnvironmentService environmentService;

    public EnvironmentController(EnvironmentService environmentService) {
        this.environmentService = environmentService;
    }

    @PostMapping
    public Environment create(
            @Valid @RequestBody CreateEnvironmentRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return environmentService.create(request, user);
    }

    @PutMapping("/{id}")
    public Environment update(
            @PathVariable("id") UUID id,
            @Valid @RequestBody UpdateEnvironmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return environmentService.update(id, request, user);
    }

    @PostMapping("/{id}/enable")
    public Environment enable(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return environmentService.enable(id, user);
    }

    @PostMapping("/{id}/disable")
    public Environment disable(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return environmentService.disable(id, user);
    }

    @PostMapping("/{id}/archive")
    public Environment archive(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return environmentService.archive(id, user);
    }

    @GetMapping
    public List<Environment> list(
            @RequestParam("projectId") UUID projectId, @AuthenticationPrincipal AuthenticatedUser user) {
        return environmentService.list(projectId, user);
    }

    @GetMapping("/{id}")
    public Environment get(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return environmentService.get(id, user);
    }

    @GetMapping("/{id}/history")
    public Environment history(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return environmentService.history(id, user);
    }
}
