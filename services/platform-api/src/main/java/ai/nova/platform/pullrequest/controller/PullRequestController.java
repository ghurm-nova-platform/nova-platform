package ai.nova.platform.pullrequest.controller;

import java.util.List;
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

import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRunRequest;
import ai.nova.platform.pullrequest.service.PullRequestAgentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/pull-requests")
@Validated
public class PullRequestController {

    private final PullRequestAgentService pullRequestAgentService;

    public PullRequestController(PullRequestAgentService pullRequestAgentService) {
        this.pullRequestAgentService = pullRequestAgentService;
    }

    @PostMapping("/run")
    public PullRequestOperation run(
            @Valid @RequestBody PullRequestRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return pullRequestAgentService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public PullRequestOperation getLatest(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return pullRequestAgentService.getLatest(taskId, user);
    }

    @GetMapping("/{taskId}/history")
    public List<PullRequestOperation> getHistory(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return pullRequestAgentService.getHistory(taskId, user);
    }
}
