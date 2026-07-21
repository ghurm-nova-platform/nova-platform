package ai.nova.platform.audit;

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
import ai.nova.platform.audit.security.AuditAuthorizationService;
import ai.nova.platform.audit.support.AuditTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = "nova.audit.enabled=true")
class AuditSecurityTest {

    @Autowired
    private AuditAuthorizationService authorizationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void readRequiresPermission() {
        authorizationService.requireRead(AuditTestFixture.auditAdminUser());
        assertThatThrownBy(() -> authorizationService.requireRead(AuditTestFixture.auditNoPermissionUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
