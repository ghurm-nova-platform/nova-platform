package ai.nova.platform.deploymentexecution;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.CreateExecutionRequest;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.service.DeploymentExecutionService;
import ai.nova.platform.deploymentexecution.support.ExecutionSeedSupport;
import ai.nova.platform.deploymentexecution.support.ExecutionTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(
        properties = {
            "nova.execution.enabled=true",
            "nova.rollback.enabled=true",
            "nova.deployment.enabled=true",
            "nova.release.enabled=true"
        })
class ExecutionConcurrencyTest {

    @Autowired
    private DeploymentExecutionService deploymentExecutionService;

    @Autowired
    private ExecutionSeedSupport seedSupport;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        user = ExecutionTestFixture.executionAdminUser();
    }

    @Test
    void blocksSecondQueuedExecutionOnSameEnvironment() throws Exception {
        var ctx = seedSupport.seedExecutionReadyContext();
        deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);

        UUID otherRelease = seedSupport.seedPublishedRelease(ExecutionTestFixture.uniqueVersion("conc"));
        assertThatThrownBy(() -> deploymentExecutionService.create(
                        new CreateExecutionRequest(
                                otherRelease,
                                ExecutionTestFixture.STAGING_ENVIRONMENT_ID,
                                null,
                                ExecutionProviderCode.LOCAL,
                                null),
                        user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("EXECUTION_CONCURRENCY_BLOCKED");
    }
}
