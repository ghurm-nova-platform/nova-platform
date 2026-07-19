package ai.nova.platform.release.controller;

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

import ai.nova.platform.release.dto.ReleaseDtos.CreateReleaseRequest;
import ai.nova.platform.release.dto.ReleaseDtos.Release;
import ai.nova.platform.release.service.ReleaseManagerService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/releases")
@Validated
public class ReleaseController {

    private final ReleaseManagerService releaseManagerService;

    public ReleaseController(ReleaseManagerService releaseManagerService) {
        this.releaseManagerService = releaseManagerService;
    }

    @PostMapping("/create")
    public Release create(@Valid @RequestBody CreateReleaseRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return releaseManagerService.create(request, user);
    }

    @PostMapping("/{id}/prepare")
    public Release prepare(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return releaseManagerService.prepare(id, user);
    }

    @PostMapping("/{id}/publish")
    public Release publish(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return releaseManagerService.publish(id, user);
    }

    @GetMapping
    public List<Release> list(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return releaseManagerService.list(projectId, user);
    }

    @GetMapping("/{id}")
    public Release get(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return releaseManagerService.get(id, user);
    }

    @GetMapping("/{id}/history")
    public Release history(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return releaseManagerService.getHistory(id, user);
    }
}
