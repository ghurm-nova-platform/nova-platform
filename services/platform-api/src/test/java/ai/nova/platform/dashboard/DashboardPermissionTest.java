package ai.nova.platform.dashboard;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.dashboard.security.DashboardAuthorizationService;
import ai.nova.platform.dashboard.support.DashboardTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = "nova.dashboard.enabled=true")
class DashboardPermissionTest {

    @Autowired
    private DashboardAuthorizationService authorizationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void readRequiresPermission() {
        authorizationService.requireRead(DashboardTestFixture.dashboardReadOnlyUser());
        assertThatThrownBy(() -> authorizationService.requireRead(DashboardTestFixture.dashboardNoPermissionUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminRequiresPermission() {
        authorizationService.requireAdmin(DashboardTestFixture.dashboardAdminUser());
        assertThatThrownBy(() -> authorizationService.requireAdmin(DashboardTestFixture.dashboardReadOnlyUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
