package ai.nova.platform.prreview.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import ai.nova.platform.prreview.config.PrReviewProperties;
import ai.nova.platform.prreview.entity.ReviewResult;
import ai.nova.platform.prreview.entity.ReviewSeverity;
import ai.nova.platform.prreview.service.ReviewRiskScoreService.ScoreBucket;
import ai.nova.platform.security.AuthenticatedUser;

@Service
public class ReviewAggregator {

    private static final Logger log = LoggerFactory.getLogger(ReviewAggregator.class);

    private final PrReviewProperties properties;
    private final ArchitectureReviewService architectureReviewService;
    private final SecurityReviewService securityReviewService;
    private final PerformanceReviewService performanceReviewService;
    private final QualityReviewService qualityReviewService;
    private final DatabaseReviewService databaseReviewService;
    private final ApiReviewService apiReviewService;
    private final DocumentationReviewService documentationReviewService;
    private final TestingReviewService testingReviewService;
    private final InfrastructureReviewService infrastructureReviewService;
    private final ReviewKnowledgeService reviewKnowledgeService;
    private final ReviewRecommendationService reviewRecommendationService;
    private final ReviewRiskScoreService reviewRiskScoreService;
    private final PrReviewMetrics metrics;

    public ReviewAggregator(
            PrReviewProperties properties,
            ArchitectureReviewService architectureReviewService,
            SecurityReviewService securityReviewService,
            PerformanceReviewService performanceReviewService,
            QualityReviewService qualityReviewService,
            DatabaseReviewService databaseReviewService,
            ApiReviewService apiReviewService,
            DocumentationReviewService documentationReviewService,
            TestingReviewService testingReviewService,
            InfrastructureReviewService infrastructureReviewService,
            ReviewKnowledgeService reviewKnowledgeService,
            ReviewRecommendationService reviewRecommendationService,
            ReviewRiskScoreService reviewRiskScoreService,
            PrReviewMetrics metrics) {
        this.properties = properties;
        this.architectureReviewService = architectureReviewService;
        this.securityReviewService = securityReviewService;
        this.performanceReviewService = performanceReviewService;
        this.qualityReviewService = qualityReviewService;
        this.databaseReviewService = databaseReviewService;
        this.apiReviewService = apiReviewService;
        this.documentationReviewService = documentationReviewService;
        this.testingReviewService = testingReviewService;
        this.infrastructureReviewService = infrastructureReviewService;
        this.reviewKnowledgeService = reviewKnowledgeService;
        this.reviewRecommendationService = reviewRecommendationService;
        this.reviewRiskScoreService = reviewRiskScoreService;
        this.metrics = metrics;
    }

    public AggregationResult analyze(String content, AuthenticatedUser user, UUID projectId) {
        return analyze(ReviewContext.of(content), user, projectId);
    }

    public AggregationResult analyze(ReviewContext context, AuthenticatedUser user, UUID projectId) {
        long started = System.nanoTime();
        List<FindingDraft> findings = collectFindings(context);
        int beforeKnowledge = findings.stream()
                .mapToInt(f -> f.knowledgeDocumentIds() == null ? 0 : f.knowledgeDocumentIds().size())
                .sum();
        findings = reviewKnowledgeService.attachKnowledge(findings, user, projectId);
        int afterKnowledge = findings.stream()
                .mapToInt(f -> f.knowledgeDocumentIds() == null ? 0 : f.knowledgeDocumentIds().size())
                .sum();
        int knowledgeHits = Math.max(0, afterKnowledge - beforeKnowledge);
        if (knowledgeHits > 0) {
            metrics.recordKnowledgeHits(knowledgeHits);
        }

        findings = capFindings(findings);
        Map<ScoreBucket, Integer> scores = reviewRiskScoreService.computeCategoryScores(findings);
        int overall = reviewRiskScoreService.overallScore(scores);
        int risk = reviewRiskScoreService.riskScore(overall);
        ReviewResult result = resolveResult(findings, overall);
        String summary = buildSummary(findings, result, overall, risk);
        List<ReviewRecommendationService.RecommendationDraft> recommendations =
                capRecommendations(reviewRecommendationService.fromFindings(findings));

        long durationMs = (System.nanoTime() - started) / 1_000_000L;
        metrics.recordReviewCompleted(durationMs, findings.size(), recommendations.size(), risk);
        log.info(
                "PR review completed result={} overall={} risk={} findings={} recommendations={} durationMs={}",
                result,
                overall,
                risk,
                findings.size(),
                recommendations.size(),
                durationMs);

        return new AggregationResult(findings, recommendations, scores, overall, risk, result, summary);
    }

    private List<FindingDraft> collectFindings(ReviewContext context) {
        List<Supplier<List<FindingDraft>>> analyzers = List.of(
                () -> architectureReviewService.analyze(context),
                () -> securityReviewService.analyze(context),
                () -> performanceReviewService.analyze(context),
                () -> qualityReviewService.analyze(context),
                () -> databaseReviewService.analyze(context),
                () -> apiReviewService.analyze(context),
                () -> documentationReviewService.analyze(context),
                () -> testingReviewService.analyze(context),
                () -> infrastructureReviewService.analyze(context));

        if (!properties.isParallelAnalysis()) {
            List<FindingDraft> findings = new ArrayList<>();
            for (Supplier<List<FindingDraft>> analyzer : analyzers) {
                findings.addAll(analyzer.get());
            }
            return findings;
        }

        ExecutorService executor = Executors.newFixedThreadPool(Math.min(9, analyzers.size()));
        try {
            List<CompletableFuture<List<FindingDraft>>> futures = analyzers.stream()
                    .map(analyzer -> CompletableFuture.supplyAsync(analyzer, executor))
                    .toList();
            List<FindingDraft> findings = new ArrayList<>();
            for (CompletableFuture<List<FindingDraft>> future : futures) {
                findings.addAll(future.join());
            }
            return findings;
        } finally {
            executor.shutdownNow();
        }
    }

    private List<FindingDraft> capFindings(List<FindingDraft> findings) {
        int max = Math.max(1, properties.getMaxFindings());
        if (findings.size() <= max) {
            return findings;
        }
        return List.copyOf(findings.subList(0, max));
    }

    private List<ReviewRecommendationService.RecommendationDraft> capRecommendations(
            List<ReviewRecommendationService.RecommendationDraft> recommendations) {
        int max = Math.max(1, properties.getMaxRecommendations());
        if (recommendations.size() <= max) {
            return recommendations;
        }
        return List.copyOf(recommendations.subList(0, max));
    }

    public ReviewResult resolveResult(List<FindingDraft> findings, int overallScore) {
        boolean hasBlocker = findings.stream().anyMatch(f -> f.severity() == ReviewSeverity.BLOCKER);
        if (hasBlocker) {
            return ReviewResult.REJECTED;
        }
        boolean hasError = findings.stream().anyMatch(f -> f.severity() == ReviewSeverity.ERROR);
        if (hasError || overallScore < 50) {
            return ReviewResult.REQUEST_CHANGES;
        }
        boolean hasSuggestionOrWarning = findings.stream()
                .anyMatch(f -> f.severity() == ReviewSeverity.WARNING || f.severity() == ReviewSeverity.SUGGESTION);
        if (hasSuggestionOrWarning) {
            return ReviewResult.APPROVED_WITH_SUGGESTIONS;
        }
        return ReviewResult.APPROVED;
    }

    public Map<ScoreBucket, Integer> computeScores(List<FindingDraft> findings) {
        return reviewRiskScoreService.computeCategoryScores(findings);
    }

    private String buildSummary(List<FindingDraft> findings, ReviewResult result, int overall, int risk) {
        long blockers = findings.stream().filter(f -> f.severity() == ReviewSeverity.BLOCKER).count();
        long errors = findings.stream().filter(f -> f.severity() == ReviewSeverity.ERROR).count();
        long warnings = findings.stream().filter(f -> f.severity() == ReviewSeverity.WARNING).count();
        return "Automated PR review result=" + result
                + ", overallScore=" + overall
                + ", riskScore=" + risk
                + ", findings=" + findings.size()
                + " (blockers=" + blockers + ", errors=" + errors + ", warnings=" + warnings + "). "
                + "Recommendations only — no merge, commit, push, fix, or GitHub approve actions were performed.";
    }

    public record AggregationResult(
            List<FindingDraft> findings,
            List<ReviewRecommendationService.RecommendationDraft> recommendations,
            Map<ReviewRiskScoreService.ScoreBucket, Integer> scores,
            int overallScore,
            int riskScore,
            ReviewResult result,
            String summary) {
    }
}
