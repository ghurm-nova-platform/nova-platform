package ai.nova.platform.prreview.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class PrReviewMetrics {

    private final Counter reviewsExecuted;
    private final Counter reviewFailures;
    private final Counter knowledgeHits;
    private final Counter exportCount;
    private final Timer reviewDuration;
    private final AtomicLong findingsTotal = new AtomicLong();
    private final AtomicLong recommendationsTotal = new AtomicLong();
    private final AtomicLong riskScoreTotal = new AtomicLong();
    private final AtomicLong completedCount = new AtomicLong();

    public PrReviewMetrics(MeterRegistry meterRegistry) {
        this.reviewsExecuted = Counter.builder("nova.pr_review.executed")
                .description("PR reviews executed")
                .register(meterRegistry);
        this.reviewFailures = Counter.builder("nova.pr_review.failures")
                .description("PR review failures")
                .register(meterRegistry);
        this.knowledgeHits = Counter.builder("nova.pr_review.knowledge_hits")
                .description("Knowledge references attached to findings")
                .register(meterRegistry);
        this.exportCount = Counter.builder("nova.pr_review.exports")
                .description("PR review exports")
                .register(meterRegistry);
        this.reviewDuration = Timer.builder("nova.pr_review.duration")
                .description("PR review analysis duration")
                .register(meterRegistry);
        meterRegistry.gauge("nova.pr_review.avg_findings", this, PrReviewMetrics::averageFindings);
        meterRegistry.gauge("nova.pr_review.avg_recommendations", this, PrReviewMetrics::averageRecommendations);
        meterRegistry.gauge("nova.pr_review.avg_risk_score", this, PrReviewMetrics::averageRiskScore);
    }

    public void recordReviewCompleted(long durationMs, int findings, int recommendations, int riskScore) {
        reviewsExecuted.increment();
        completedCount.incrementAndGet();
        findingsTotal.addAndGet(findings);
        recommendationsTotal.addAndGet(recommendations);
        riskScoreTotal.addAndGet(riskScore);
        reviewDuration.record(java.time.Duration.ofMillis(Math.max(0, durationMs)));
    }

    public void recordFailure() {
        reviewFailures.increment();
    }

    public void recordKnowledgeHits(int hits) {
        if (hits > 0) {
            knowledgeHits.increment(hits);
        }
    }

    public void recordExport() {
        exportCount.increment();
    }

    private double averageFindings() {
        long completed = completedCount.get();
        return completed == 0 ? 0.0 : (double) findingsTotal.get() / completed;
    }

    private double averageRecommendations() {
        long completed = completedCount.get();
        return completed == 0 ? 0.0 : (double) recommendationsTotal.get() / completed;
    }

    private double averageRiskScore() {
        long completed = completedCount.get();
        return completed == 0 ? 0.0 : (double) riskScoreTotal.get() / completed;
    }
}
