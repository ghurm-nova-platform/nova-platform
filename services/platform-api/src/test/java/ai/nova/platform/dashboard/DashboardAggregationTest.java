package ai.nova.platform.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.dashboard.entity.PipelineStageCode;
import ai.nova.platform.dashboard.service.DashboardAggregationService;
import ai.nova.platform.dashboard.support.DashboardTestFixture;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;

@SpringBootTest(properties = "nova.dashboard.enabled=true")
class DashboardAggregationTest {

    @Autowired
    private DashboardAggregationService aggregationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void aggregatesOverviewAndPipeline() {
        var overview = aggregationService.aggregateOverview(DashboardTestFixture.dashboardReadOnlyUser(), null);
        assertThat(overview.projectCount()).isGreaterThanOrEqualTo(0);

        var pipeline = aggregationService.aggregatePipeline(DashboardTestFixture.dashboardReadOnlyUser(), null);
        assertThat(pipeline.stages()).hasSize(PipelineStageCode.values().length);
        assertThat(pipeline.stages().stream().map(stage -> stage.stage()).toList())
                .contains(PipelineStageCode.PLANNER, PipelineStageCode.ROLLBACK);
    }

    @Test
    void resolvesPipelineStageFromTaskKey() {
        AgentOrchestrationTask task = new AgentOrchestrationTask(
                java.util.UUID.randomUUID(),
                DashboardTestFixture.ORG_ID,
                DashboardTestFixture.PROJECT_ID,
                java.util.UUID.randomUUID(),
                "ci-observe",
                "CI Observation",
                TaskType.AGENT_TURN,
                TaskStatus.RUNNING,
                "idem",
                3,
                1000,
                1,
                60,
                DashboardTestFixture.USER_ID,
                java.time.Instant.now());
        assertThat(DashboardAggregationService.resolveStage(task)).isEqualTo(PipelineStageCode.CI);
    }
}
