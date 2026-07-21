package ai.nova.platform.prreview.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
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

import ai.nova.platform.prreview.dto.PrReviewDtos.ExportPayload;
import ai.nova.platform.prreview.dto.PrReviewDtos.ExportRequest;
import ai.nova.platform.prreview.dto.PrReviewDtos.FindingView;
import ai.nova.platform.prreview.dto.PrReviewDtos.KnowledgeReferenceView;
import ai.nova.platform.prreview.dto.PrReviewDtos.PrReviewConfigResponse;
import ai.nova.platform.prreview.dto.PrReviewDtos.RecommendationView;
import ai.nova.platform.prreview.dto.PrReviewDtos.RerunRequest;
import ai.nova.platform.prreview.dto.PrReviewDtos.ReviewRunDetail;
import ai.nova.platform.prreview.dto.PrReviewDtos.ReviewRunSummary;
import ai.nova.platform.prreview.dto.PrReviewDtos.RiskScoreView;
import ai.nova.platform.prreview.dto.PrReviewDtos.RunRequest;
import ai.nova.platform.prreview.service.PullRequestReviewService;
import ai.nova.platform.security.AuthenticatedUser;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/pr-review")
@Validated
public class PullRequestReviewController {

    private final PullRequestReviewService reviewService;

    public PullRequestReviewController(PullRequestReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/config")
    public PrReviewConfigResponse config(@AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.config(user);
    }

    @GetMapping
    public List<ReviewRunSummary> list(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.list(projectId, user);
    }

    @GetMapping("/history")
    public List<ReviewRunSummary> history(
            @RequestParam(value = "projectId", required = false) UUID projectId,
            @RequestParam(value = "pullRequestOperationId", required = false) UUID pullRequestOperationId,
            @RequestParam(value = "pullRequestNumber", required = false) Integer pullRequestNumber,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.history(projectId, pullRequestOperationId, pullRequestNumber, user);
    }

    @GetMapping("/{id}")
    public ReviewRunDetail get(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.get(id, user);
    }

    @GetMapping("/{id}/findings")
    public List<FindingView> findings(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.findings(id, user);
    }

    @GetMapping("/{id}/recommendations")
    public List<RecommendationView> recommendations(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.recommendations(id, user);
    }

    @GetMapping("/{id}/risk-score")
    public RiskScoreView riskScore(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.riskScore(id, user);
    }

    @GetMapping("/{id}/knowledge")
    public List<KnowledgeReferenceView> knowledge(
            @PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.knowledge(id, user);
    }

    @GetMapping("/{id}/export")
    public ResponseEntity<byte[]> exportGet(
            @PathVariable UUID id,
            @RequestParam(value = "format", defaultValue = "markdown") String format,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return toExportResponse(reviewService.export(id, format, user));
    }

    @PostMapping("/{id}/export")
    public ResponseEntity<byte[]> exportPost(
            @PathVariable UUID id,
            @RequestBody(required = false) ExportRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        String format = request == null || request.format() == null ? "markdown" : request.format();
        return toExportResponse(reviewService.export(id, format, user));
    }

    @PostMapping("/run")
    public ReviewRunDetail run(
            @Valid @RequestBody RunRequest request, @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.run(request, user);
    }

    @PostMapping("/{id}/rerun")
    public ReviewRunDetail rerun(
            @PathVariable UUID id,
            @RequestBody(required = false) RerunRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        return reviewService.rerun(
                id, request == null ? new RerunRequest(null, null, null) : request, user);
    }

    private ResponseEntity<byte[]> toExportResponse(ExportPayload payload) {
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + payload.fileName() + "\"")
                .contentType(MediaType.parseMediaType(payload.contentType()))
                .body(payload.content());
    }
}
