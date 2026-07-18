package ai.nova.platform.review.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record ReviewRunRequest(@NotNull UUID taskId) {
    }

    public record ReviewFindingDraft(
            ReviewSeverity severity,
            ReviewCategory category,
            String title,
            String description,
            String recommendation,
            String artifactPath) {
    }

    public record ParsedReviewOutput(
            String summary, Integer score, Boolean approved, List<ReviewFindingDraft> findings) {
    }

    public record ReviewFinding(
            UUID id,
            ReviewSeverity severity,
            ReviewCategory category,
            String title,
            String description,
            String recommendation,
            UUID artifactId,
            String artifactPath) {
    }

    public record ArtifactReview(
            UUID artifactId, String path, String filename, String language, String sha256) {
    }

    public record ReviewRecommendation(String title, String recommendation, ReviewSeverity severity) {
    }

    public record ReviewResult(
            UUID id,
            UUID taskId,
            UUID runId,
            UUID projectId,
            String summary,
            int score,
            boolean approved,
            List<ReviewFinding> findings,
            List<ArtifactReview> reviewedArtifacts,
            Map<String, Long> severityCounts,
            Long tokensUsed,
            String model,
            String provider,
            Long reviewTimeMs,
            Instant createdAt,
            boolean validated) {
    }

    public record ReviewPromptContext(
            UUID taskId,
            String taskKey,
            String displayName,
            String description,
            String objective,
            List<GeneratedArtifactResponse> artifacts,
            Map<String, String> organizationSettings,
            Map<String, String> projectSettings) {
    }
}
