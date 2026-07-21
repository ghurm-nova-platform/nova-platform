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
import ai.nova.platform.dashboard.service.DashboardCacheService;
import ai.nova.platform.dashboard.service.DashboardService;
import ai.nova.platform.dashboard.support.DashboardTestFixture;

@SpringBootTest(properties = "nova.dashboard.enabled=true")
class DashboardPerformanceTest {

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private DashboardCacheService cacheService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        cacheService.invalidateOrganization(DashboardTestFixture.ORG_ID);
    }

    @Test
    void cachedSnapshotIsFasterThanColdBuild() {
        long coldStart = System.nanoTime();
        dashboardService.getSnapshot(DashboardTestFixture.dashboardReadOnlyUser(), null);
        long coldElapsedMs = (System.nanoTime() - coldStart) / 1_000_000;

        long warmStart = System.nanoTime();
        dashboardService.getSnapshot(DashboardTestFixture.dashboardReadOnlyUser(), null);
        long warmElapsedMs = (System.nanoTime() - warmStart) / 1_000_000;

        assertThat(warmElapsedMs).isLessThanOrEqualTo(coldElapsedMs + 50);
    }
}
