package ai.nova.platform.deploymentexecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.CreateExecutionRequest;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.provider.LocalExecutionProvider;
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
class ExecutionCancellationTest {

    @Autowired
    private DeploymentExecutionService deploymentExecutionService;

    @Autowired
    private ExecutionSeedSupport seedSupport;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @MockitoSpyBean
    private LocalExecutionProvider localExecutionProvider;

    private final AtomicBoolean blockDeploy = new AtomicBoolean(false);
    private final AtomicBoolean cancelCalled = new AtomicBoolean(false);
    private volatile CountDownLatch enteredDeploy = new CountDownLatch(1);
    private volatile CountDownLatch releaseDeploy = new CountDownLatch(1);

    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        user = ExecutionTestFixture.executionAdminUser();
        blockDeploy.set(false);
        cancelCalled.set(false);
        enteredDeploy = new CountDownLatch(1);
        releaseDeploy = new CountDownLatch(1);

        doAnswer(invocation -> {
                    if (blockDeploy.get()) {
                        enteredDeploy.countDown();
                        if (!releaseDeploy.await(15, TimeUnit.SECONDS)) {
                            throw new IllegalStateException("Deploy latch timeout");
                        }
                    }
                    return invocation.callRealMethod();
                })
                .when(localExecutionProvider)
                .deploy(any());
        doAnswer(invocation -> {
                    cancelCalled.set(true);
                    releaseDeploy.countDown();
                    return invocation.callRealMethod();
                })
                .when(localExecutionProvider)
                .cancel(any());
    }

    @Test
    void cancelQueuedExecution() throws Exception {
        var ctx = seedSupport.seedExecutionReadyContext();
        var created = deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);
        var cancelled = deploymentExecutionService.cancel(created.id(), user);
        assertThat(cancelled.status()).isEqualTo(ExecutionStatus.CANCELLED);
    }

    @Test
    void cancelRunningExecution() throws Exception {
        blockDeploy.set(true);
        var ctx = seedSupport.seedExecutionReadyContext();
        var created = deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);
        deploymentExecutionService.start(created.id(), user);
        assertThat(enteredDeploy.await(5, TimeUnit.SECONDS)).isTrue();

        deploymentExecutionService.cancel(created.id(), user);
        ExecutionTestFixture.awaitStatus(
                () -> deploymentExecutionService.get(created.id(), user).status(),
                ExecutionStatus.CANCELLED,
                10_000);
        assertThat(deploymentExecutionService.get(created.id(), user).status()).isEqualTo(ExecutionStatus.CANCELLED);
        assertThat(cancelCalled.get()).isTrue();
    }

    @Test
    void cancelAfterCompletionRejected() throws Exception {
        blockDeploy.set(false);
        var ctx = seedSupport.seedExecutionReadyContext();
        var created = deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);
        deploymentExecutionService.start(created.id(), user);
        ExecutionTestFixture.awaitStatus(
                () -> deploymentExecutionService.get(created.id(), user).status(),
                ExecutionStatus.COMPLETED,
                10_000);

        assertThatThrownBy(() -> deploymentExecutionService.cancel(created.id(), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("EXECUTION_INVALID_STATUS");
    }

    @Test
    void duplicateStartRaceReturnsAlreadyStarted() throws Exception {
        blockDeploy.set(true);
        var ctx = seedSupport.seedExecutionReadyContext();
        var created = deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);
        deploymentExecutionService.start(created.id(), user);
        assertThatThrownBy(() -> deploymentExecutionService.start(created.id(), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("EXECUTION_ALREADY_STARTED");
        deploymentExecutionService.cancel(created.id(), user);
        ExecutionTestFixture.awaitTerminal(
                () -> deploymentExecutionService.get(created.id(), user).status(), 10_000);
    }
}
