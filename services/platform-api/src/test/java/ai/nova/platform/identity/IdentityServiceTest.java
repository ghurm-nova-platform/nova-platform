package ai.nova.platform.identity;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.identity.dto.IdentityDtos.ConfigResponse;
import ai.nova.platform.identity.dto.IdentityDtos.DashboardView;
import ai.nova.platform.identity.dto.IdentityDtos.SummaryView;
import ai.nova.platform.identity.service.IdentityService;
import ai.nova.platform.identity.support.IdentityTestFixture;

@SpringBootTest
class IdentityServiceTest {

    @Autowired
    private IdentityService identityService;

    @Test
    @Transactional
    void configSummaryAndDashboard() {
        ConfigResponse config = identityService.getConfig();
        assertThat(config.enabled()).isTrue();
        assertThat(config.mfaEnabled()).isTrue();

        SummaryView summary = identityService.summary(IdentityTestFixture.ORG_ID);
        assertThat(summary.users()).isNotEmpty();
        assertThat(summary.providers()).isNotEmpty();

        DashboardView dashboard = identityService.dashboard(IdentityTestFixture.ORG_ID);
        assertThat(dashboard.activeUsers()).isNotNegative();
        assertThat(dashboard.providerCount()).isPositive();
    }
}
