package ai.nova.platform.collaboration;

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
import ai.nova.platform.collaboration.security.CollaborationAuthorizationService;
import ai.nova.platform.collaboration.support.CollaborationTestFixture;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = {"nova.collaboration.enabled=true", "nova.audit.enabled=true"})
class CollaborationPermissionTest {

    @Autowired
    private CollaborationAuthorizationService authorizationService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void readRequiresPermission() {
        authorizationService.requireRead(CollaborationTestFixture.collaborationReadUser());
        assertThatThrownBy(() -> authorizationService.requireRead(CollaborationTestFixture.collaborationNoPermissionUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void writeRequiresPermission() {
        authorizationService.requireWrite(CollaborationTestFixture.collaborationWriteUser());
        assertThatThrownBy(() -> authorizationService.requireWrite(CollaborationTestFixture.collaborationReadUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void adminRequiresPermission() {
        authorizationService.requireAdmin(CollaborationTestFixture.collaborationAdminUser());
        assertThatThrownBy(() -> authorizationService.requireAdmin(CollaborationTestFixture.collaborationWriteUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getStatus())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }
}
