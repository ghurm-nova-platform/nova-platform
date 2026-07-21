package ai.nova.platform.deploymentexecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.service.ExecutionValidationService;
import ai.nova.platform.deploymentexecution.support.ExecutionSeedSupport;
import ai.nova.platform.deploymentexecution.support.ExecutionTestFixture;

@SpringBootTest(
        properties = {
            "nova.execution.enabled=true",
            "nova.rollback.enabled=true",
            "nova.deployment.enabled=true",
            "nova.release.enabled=true"
        })
class ExecutionValidationTest {

    @Autowired
    private ExecutionValidationService validationService;

    @Autowired
    private ExecutionSeedSupport seedSupport;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void passesWhenReleaseRollbackAndEnvironmentReady() throws Exception {
        var ctx = seedSupport.seedExecutionReadyContext();
        var outcome = validationService.validateForCreate(
                ExecutionTestFixture.ORG_ID,
                ctx.releaseId(),
                ExecutionTestFixture.STAGING_ENVIRONMENT_ID,
                ExecutionProviderCode.LOCAL);
        assertThat(outcome.passed()).isTrue();
    }

    @Test
    void failsWhenReleaseMissing() {
        var outcome = validationService.validateForCreate(
                ExecutionTestFixture.ORG_ID,
                UUID.randomUUID(),
                ExecutionTestFixture.STAGING_ENVIRONMENT_ID,
                ExecutionProviderCode.LOCAL);
        assertThat(outcome.passed()).isFalse();
        assertThat(outcome.errorCode()).isEqualTo("EXECUTION_RELEASE_NOT_FOUND");
    }
}
