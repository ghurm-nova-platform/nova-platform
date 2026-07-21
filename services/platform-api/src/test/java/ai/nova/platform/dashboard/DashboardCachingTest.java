package ai.nova.platform.dashboard;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.dashboard.config.DashboardProperties;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardMeta;
import ai.nova.platform.dashboard.dto.DashboardDtos.DashboardSnapshot;
import ai.nova.platform.dashboard.service.DashboardCacheService;
import ai.nova.platform.dashboard.support.DashboardTestFixture;

@SpringBootTest(properties = "nova.dashboard.enabled=true")
class DashboardCachingTest {

    @Autowired
    private DashboardCacheService cacheService;

    @Autowired
    private DashboardProperties properties;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        cacheService.invalidateOrganization(DashboardTestFixture.ORG_ID);
    }

    @Test
    void cachesUntilTtlExpires() {
        DashboardSnapshot snapshot = minimalSnapshot();
        var cached = cacheService.put(DashboardTestFixture.ORG_ID, null, snapshot);
        assertThat(cached.fromCache()).isFalse();
        assertThat(cacheService.get(DashboardTestFixture.ORG_ID, null)).isPresent();
    }

    @Test
    void invalidatesOrganizationEntries() {
        cacheService.put(DashboardTestFixture.ORG_ID, null, minimalSnapshot());
        cacheService.put(DashboardTestFixture.ORG_ID, DashboardTestFixture.PROJECT_ID, minimalSnapshot());
        cacheService.invalidateOrganization(DashboardTestFixture.ORG_ID);
        assertThat(cacheService.get(DashboardTestFixture.ORG_ID, null)).isEmpty();
        assertThat(properties.getCache().getTtlSeconds()).isEqualTo(30);
    }

    private DashboardSnapshot minimalSnapshot() {
        return new DashboardSnapshot(
                new DashboardMeta(
                        DashboardTestFixture.ORG_ID,
                        null,
                        Instant.now(),
                        Instant.now().plusSeconds(30),
                        30,
                        false),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null);
    }
}
