package ai.nova.platform.prreview;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.prreview.service.ReviewAggregator;
import ai.nova.platform.prreview.service.ReviewAggregator.AggregationResult;
import ai.nova.platform.prreview.service.ReviewContext;
import ai.nova.platform.prreview.support.PrReviewTestFixture;

@SpringBootTest(properties = {"nova.pr-review.enabled=true", "nova.audit.enabled=true", "nova.pr-review.parallel-analysis=false"})
class PrReviewPerformanceTest {

    @Autowired
    private ReviewAggregator reviewAggregator;

    @Test
    void largeSyntheticDiffCompletesUnderBudget() {
        String largeDiff = IntStream.range(0, 1200)
                .mapToObj(i -> "public class Generated" + i + " { int value = " + i + "; }\n")
                .collect(Collectors.joining());
        var files = IntStream.range(0, 500).mapToObj(i -> "src/Generated" + i + ".java").toList();
        long started = System.nanoTime();
        AggregationResult result = reviewAggregator.analyze(
                new ReviewContext(largeDiff, files, "deadbeef"),
                PrReviewTestFixture.prReviewRunUser(),
                PrReviewTestFixture.PROJECT_ID);
        long elapsedMs = (System.nanoTime() - started) / 1_000_000L;
        assertThat(result).isNotNull();
        assertThat(elapsedMs).isLessThan(15_000L);
    }
}
