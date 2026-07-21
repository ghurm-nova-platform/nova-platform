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
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardSnapshot;
import ai.nova.platform.dashboard.service.DashboardCacheService;
import ai.nova.platform.dashboard.service.DashboardService;
import ai.nova.platform.dashboard.support.DashboardTestFixture;

@SpringBootTest(properties = "nova.dashboard.enabled=true")
class DashboardServiceTest {

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
        cacheService.invalidateOrganization(DashboardTestFixture.OTHER_ORG_ID);
    }

    @Test
    void returnsFullSnapshot() {
        DashboardSnapshot snapshot = dashboardService.getSnapshot(DashboardTestFixture.dashboardReadOnlyUser(), null);
        assertThat(snapshot.meta().organizationId()).isEqualTo(DashboardTestFixture.ORG_ID);
        assertThat(snapshot.overview()).isNotNull();
        assertThat(snapshot.pipeline().stages()).hasSize(14);
        assertThat(snapshot.cost().note()).contains("placeholder");
    }

    @Test
    void cachesAreIsolatedPerOrganization() {
        DashboardSnapshot first = dashboardService.getSnapshot(DashboardTestFixture.dashboardReadOnlyUser(), null);
        DashboardSnapshot second = dashboardService.getSnapshot(DashboardTestFixture.dashboardOtherOrgReadOnlyUser(), null);

        assertThat(first.meta().organizationId()).isEqualTo(DashboardTestFixture.ORG_ID);
        assertThat(second.meta().organizationId()).isEqualTo(DashboardTestFixture.OTHER_ORG_ID);
        assertThat(cacheService.size()).isGreaterThanOrEqualTo(2);
        assertThat(cacheService.get(DashboardTestFixture.ORG_ID, null)).isPresent();
        assertThat(cacheService.get(DashboardTestFixture.OTHER_ORG_ID, null)).isPresent();
    }

    @Test
    void refreshInvalidatesOnlyOwnOrganizationCache() {
        dashboardService.getSnapshot(DashboardTestFixture.dashboardReadOnlyUser(), null);
        dashboardService.getSnapshot(DashboardTestFixture.dashboardOtherOrgReadOnlyUser(), null);

        assertThat(cacheService.get(DashboardTestFixture.ORG_ID, null)).isPresent();
        assertThat(cacheService.get(DashboardTestFixture.OTHER_ORG_ID, null)).isPresent();

        dashboardService.refresh(DashboardTestFixture.dashboardAdminUser(), null);

        assertThat(cacheService.get(DashboardTestFixture.ORG_ID, null)).isPresent();
        assertThat(cacheService.get(DashboardTestFixture.OTHER_ORG_ID, null)).isPresent();
    }
}
