package ai.nova.platform.rollback.controller;

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

import ai.nova.platform.rollback.dto.RollbackDtos.CreateRollbackRequest;
import ai.nova.platform.rollback.dto.RollbackDtos.Rollback;
import ai.nova.platform.rollback.service.RollbackManagerService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/rollbacks")
@Validated
public class RollbackController {

    private final RollbackManagerService rollbackManagerService;

    public RollbackController(RollbackManagerService rollbackManagerService) {
        this.rollbackManagerService = rollbackManagerService;
    }

    @PostMapping("/create")
    public Rollback create(
            @Valid @RequestBody CreateRollbackRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return rollbackManagerService.create(request, user);
    }

    @PostMapping("/{id}/validate")
    public Rollback validate(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return rollbackManagerService.validate(id, user);
    }

    @GetMapping
    public List<Rollback> list(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return rollbackManagerService.list(projectId, user);
    }

    @GetMapping("/{id}")
    public Rollback get(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return rollbackManagerService.get(id, user);
    }

    @GetMapping("/{id}/history")
    public Rollback history(@PathVariable("id") UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return rollbackManagerService.history(id, user);
    }
}
