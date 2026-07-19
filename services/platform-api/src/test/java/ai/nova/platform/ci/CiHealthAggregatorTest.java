package ai.nova.platform.ci;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.provider.ProviderJob;
import ai.nova.platform.ci.provider.ProviderStep;
import ai.nova.platform.ci.provider.ProviderWorkflowRun;
import ai.nova.platform.ci.service.CiHealthAggregator;

class CiHealthAggregatorTest {

    private final CiHealthAggregator aggregator = new CiHealthAggregator();

    @Test
    void aggregatesSuccessWhenAllRunsPass() {
        Instant now = Instant.now();
        ProviderWorkflowRun run = new ProviderWorkflowRun(
                "wf-1",
                "CI",
                "run-1",
                "http://ci/run-1",
                "completed",
                "success",
                1000L,
                "pull_request",
                "abc",
                "main",
                1L,
                null,
                now.minusSeconds(60),
                now);
        ProviderJob job = new ProviderJob(
                "job-1",
                "build",
                "completed",
                "success",
                500L,
                null,
                List.of(new ProviderStep(1, "compile", "completed", "success", 100L, null, now, now)),
                now.minusSeconds(30),
                now);

        var health = aggregator.aggregate(List.of(run), List.of(List.of(job)));

        assertThat(health.overallStatus()).isEqualTo(CiOverallStatus.SUCCESS);
        assertThat(health.failureSummary().failedWorkflows()).isZero();
        assertThat(health.retryRecommendation()).contains("no retry needed");
    }

    @Test
    void aggregatesFailureWithRetryRecommendation() {
        Instant now = Instant.now();
        ProviderWorkflowRun run = new ProviderWorkflowRun(
                "wf-1",
                "CI",
                "run-1",
                "http://ci/run-1",
                "completed",
                "failure",
                1000L,
                "pull_request",
                "abc",
                "main",
                1L,
                "Workflow failed",
                now.minusSeconds(60),
                now);
        ProviderJob job = new ProviderJob(
                "job-1",
                "test",
                "completed",
                "failure",
                500L,
                "Job failed",
                List.of(new ProviderStep(
                        1, "run tests", "completed", "failure", 100L, "Step failed", now.minusSeconds(10), now)),
                now.minusSeconds(30),
                now);

        var health = aggregator.aggregate(List.of(run), List.of(List.of(job)));

        assertThat(health.overallStatus()).isEqualTo(CiOverallStatus.FAILED);
        assertThat(health.failureSummary().failedWorkflows()).isEqualTo(1);
        assertThat(health.failureSummary().failedJobs()).isEqualTo(1);
        assertThat(health.failureSummary().failedSteps()).isEqualTo(1);
        assertThat(health.retryRecommendation()).isEqualTo("Investigate failed jobs; do not auto-rerun from Nova");
        assertThat(health.failureSummaryText()).contains("Failed workflows: 1");
    }

    @Test
    void inProgressWhenRunsStillRunning() {
        ProviderWorkflowRun run = new ProviderWorkflowRun(
                "wf-1",
                "CI",
                "run-1",
                "http://ci/run-1",
                "in_progress",
                null,
                null,
                "pull_request",
                "abc",
                "main",
                1L,
                null,
                Instant.now(),
                null);

        var health = aggregator.aggregate(List.of(run), List.of(List.of()));

        assertThat(health.overallStatus()).isEqualTo(CiOverallStatus.IN_PROGRESS);
        assertThat(health.retryRecommendation()).contains("still running");
    }

    @Test
    void unknownWhenNoRunsFound() {
        var health = aggregator.aggregate(List.of(), List.of());

        assertThat(health.overallStatus()).isEqualTo(CiOverallStatus.UNKNOWN);
    }
}
