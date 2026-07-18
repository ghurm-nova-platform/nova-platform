package ai.nova.platform.review.controller;

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

import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.review.dto.ReviewDtos.ReviewRunRequest;
import ai.nova.platform.review.service.ReviewAgentService;
import ai.nova.platform.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/review")
@Validated
public class ReviewController {

    private final ReviewAgentService reviewAgentService;

    public ReviewController(ReviewAgentService reviewAgentService) {
        this.reviewAgentService = reviewAgentService;
    }

    @PostMapping("/run")
    public ReviewResult run(
            @Valid @RequestBody ReviewRunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewAgentService.run(request, user);
    }

    @GetMapping("/{taskId}")
    public ReviewResult getLatest(
            @PathVariable UUID taskId, @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewAgentService.getLatest(taskId, user);
    }
}
