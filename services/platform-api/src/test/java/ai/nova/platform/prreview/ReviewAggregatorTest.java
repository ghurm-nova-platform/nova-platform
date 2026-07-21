package ai.nova.platform.prreview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.prreview.entity.ReviewResult;
import ai.nova.platform.prreview.entity.ReviewSeverity;
import ai.nova.platform.prreview.service.FindingDraft;
import ai.nova.platform.prreview.service.ReviewAggregator;
import ai.nova.platform.prreview.service.ReviewAggregator.AggregationResult;
import ai.nova.platform.prreview.service.ReviewRiskScoreService.ScoreBucket;
import ai.nova.platform.prreview.support.PrReviewTestFixture;

@SpringBootTest(properties = {"nova.pr-review.enabled=true", "nova.audit.enabled=true"})
class ReviewAggregatorTest {

    @Autowired
    private ReviewAggregator reviewAggregator;

    @Test
    void cleanDiffIsApproved() {
        AggregationResult result = reviewAggregator.analyze(
                PrReviewTestFixture.cleanDiff(), PrReviewTestFixture.prReviewRunUser(), PrReviewTestFixture.PROJECT_ID);
        assertThat(result.result()).isEqualTo(ReviewResult.APPROVED);
        assertThat(result.overallScore()).isEqualTo(100);
        assertThat(result.riskScore()).isZero();
        assertThat(result.findings()).isEmpty();
    }

    @Test
    void blockerDiffIsRejected() {
        AggregationResult result = reviewAggregator.analyze(
                PrReviewTestFixture.securityDiff(),
                PrReviewTestFixture.prReviewRunUser(),
                PrReviewTestFixture.PROJECT_ID);
        assertThat(result.findings()).isNotEmpty();
        assertThat(result.findings().stream().map(FindingDraft::severity)).contains(ReviewSeverity.BLOCKER);
        assertThat(result.result()).isEqualTo(ReviewResult.REJECTED);
        assertThat(result.overallScore()).isLessThan(100);
        assertThat(result.riskScore()).isGreaterThan(0);
        assertThat(result.recommendations()).isNotEmpty();
    }

    @Test
    void scorePenaltiesFloorAtZero() {
        List<FindingDraft> findings = List.of(
                new FindingDraft(
                        ai.nova.platform.prreview.entity.ReviewCategory.Security,
                        ReviewSeverity.BLOCKER,
                        "a",
                        "b",
                        "c"),
                new FindingDraft(
                        ai.nova.platform.prreview.entity.ReviewCategory.Security,
                        ReviewSeverity.BLOCKER,
                        "d",
                        "e",
                        "f"),
                new FindingDraft(
                        ai.nova.platform.prreview.entity.ReviewCategory.Security,
                        ReviewSeverity.BLOCKER,
                        "g",
                        "h",
                        "i"));
        var scores = reviewAggregator.computeScores(findings);
        assertThat(scores.get(ScoreBucket.SECURITY)).isZero();
        assertThat(reviewAggregator.resolveResult(findings, 10)).isEqualTo(ReviewResult.REJECTED);
    }
}
