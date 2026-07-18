package ai.nova.platform.git.controller;

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

import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.dto.GitDtos.GitRunRequest;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/git")
@Validated
public class GitController {

    private final GitIntegrationAgentService gitIntegrationAgentService;

    public GitController(GitIntegrationAgentService gitIntegrationAgentService) {
        this.gitIntegrationAgentService = gitIntegrationAgentService;
    }

    @PostMapping("/run")
    public GitOperation run(
            @Valid @RequestBody GitRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return gitIntegrationAgentService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public GitOperation getLatest(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return gitIntegrationAgentService.getLatest(taskId, user);
    }
}
