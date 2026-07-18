package ai.nova.platform.patch.controller;

import java.util.UUID;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.dto.PatchDtos.PatchRunRequest;
import ai.nova.platform.patch.service.PatchAgentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/patch")
@Validated
public class PatchController {

    private final PatchAgentService patchAgentService;

    public PatchController(PatchAgentService patchAgentService) {
        this.patchAgentService = patchAgentService;
    }

    @PostMapping("/run")
    public PatchResult run(
            @Valid @RequestBody PatchRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return patchAgentService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public PatchResult getLatest(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return patchAgentService.getLatest(taskId, user);
    }
}
