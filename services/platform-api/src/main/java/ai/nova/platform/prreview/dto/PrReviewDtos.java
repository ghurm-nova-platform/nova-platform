package ai.nova.platform.prreview.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import ai.nova.platform.prreview.entity.RecommendationPriority;
import ai.nova.platform.prreview.entity.ReviewCategory;
import ai.nova.platform.prreview.entity.ReviewResult;
import ai.nova.platform.prreview.entity.ReviewRunStatus;
import ai.nova.platform.prreview.entity.ReviewSeverity;

public final class PrReviewDtos {

    private PrReviewDtos() {
    }

    public record PrReviewConfigResponse(
            boolean enabled,
            int maxDiffCharacters,
            int defaultLimit,
            int maxFindings,
            int maxRecommendations,
            boolean parallelAnalysis,
            boolean exportEnabled,
            boolean cacheEnabled,
            int cacheTtlSeconds) {
    }

    public record RunRequest(
            @NotNull UUID projectId,
            UUID pullRequestOperationId,
            Integer pullRequestNumber,
            @Size(max = 500) String pullRequestTitle,
            @Size(max = 500) String repositoryRef,
            @Size(max = 255) String sourceBranch,
            @Size(max = 255) String targetBranch,
            @Size(max = 100) String commitSha,
            List<String> changedFiles,
            @NotBlank String diffContent) {
    }

    public record RerunRequest(String diffContent, List<String> changedFiles, String commitSha) {
    }

    public record ExportRequest(@Size(max = 20) String format) {
    }

    public record ReviewRunSummary(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID pullRequestOperationId,
            Integer pullRequestNumber,
            String pullRequestTitle,
            String repositoryRef,
            String sourceBranch,
            String targetBranch,
            String commitSha,
            ReviewRunStatus status,
            ReviewResult result,
            int overallScore,
            int riskScore,
            int architectureScore,
            int securityScore,
            int performanceScore,
            int qualityScore,
            int testingScore,
            int documentationScore,
            String summary,
            UUID createdBy,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ReviewRunDetail(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID pullRequestOperationId,
            Integer pullRequestNumber,
            String pullRequestTitle,
            String repositoryRef,
            String sourceBranch,
            String targetBranch,
            String commitSha,
            List<String> changedFiles,
            ReviewRunStatus status,
            ReviewResult result,
            int overallScore,
            int riskScore,
            int architectureScore,
            int securityScore,
            int performanceScore,
            int qualityScore,
            int testingScore,
            int documentationScore,
            String summary,
            String diffExcerpt,
            UUID createdBy,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt,
            List<FindingView> findings,
            List<RecommendationView> recommendations) {
    }

    public record FindingView(
            UUID id,
            UUID reviewRunId,
            ReviewCategory category,
            ReviewSeverity severity,
            String title,
            String description,
            String recommendation,
            String filePath,
            Integer lineHint,
            String ruleCode,
            String evidenceExcerpt,
            List<String> references,
            List<UUID> knowledgeDocumentIds,
            Instant createdAt) {
    }

    public record RecommendationView(
            UUID id,
            UUID reviewRunId,
            UUID findingId,
            RecommendationPriority priority,
            String title,
            String description,
            List<UUID> knowledgeDocumentIds,
            Instant createdAt) {
    }

    public record RiskScoreView(
            UUID reviewRunId,
            int overallScore,
            int riskScore,
            ReviewResult result,
            Map<String, Integer> categoryScores) {
    }

    public record KnowledgeReferenceView(
            UUID findingId,
            String findingTitle,
            ReviewCategory category,
            List<UUID> knowledgeDocumentIds) {
    }

    public record ExportPayload(byte[] content, String contentType, String fileName) {
    }
}
