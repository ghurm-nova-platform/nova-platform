package ai.nova.platform.deploymentexecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.CreateExecutionRequest;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.provider.LocalExecutionProvider;
import ai.nova.platform.deploymentexecution.service.DeploymentExecutionService;
import ai.nova.platform.deploymentexecution.support.ExecutionSeedSupport;
import ai.nova.platform.deploymentexecution.support.ExecutionTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest(
        properties = {
            "nova.execution.enabled=true",
            "nova.rollback.enabled=true",
            "nova.deployment.enabled=true",
            "nova.release.enabled=true"
        })
class ExecutionTransactionTest {

    @Autowired
    private DeploymentExecutionService deploymentExecutionService;

    @Autowired
    private ExecutionSeedSupport seedSupport;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @MockitoSpyBean
    private LocalExecutionProvider localExecutionProvider;

    private final AtomicBoolean prepareSawTx = new AtomicBoolean(false);
    private final AtomicBoolean deploySawTx = new AtomicBoolean(false);
    private final AtomicBoolean verifySawTx = new AtomicBoolean(false);

    private AuthenticatedUser user;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        user = ExecutionTestFixture.executionAdminUser();
        prepareSawTx.set(false);
        deploySawTx.set(false);
        verifySawTx.set(false);

        doAnswer(invocation -> {
                    prepareSawTx.set(TransactionSynchronizationManager.isActualTransactionActive());
                    return invocation.callRealMethod();
                })
                .when(localExecutionProvider)
                .prepare(any());
        doAnswer(invocation -> {
                    deploySawTx.set(TransactionSynchronizationManager.isActualTransactionActive());
                    return invocation.callRealMethod();
                })
                .when(localExecutionProvider)
                .deploy(any());
        doAnswer(invocation -> {
                    verifySawTx.set(TransactionSynchronizationManager.isActualTransactionActive());
                    return invocation.callRealMethod();
                })
                .when(localExecutionProvider)
                .verify(any());
    }

    @Test
    void providerMethodsDoNotRunInsideOpenTransaction() throws Exception {
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

        assertThat(prepareSawTx.get()).isFalse();
        assertThat(deploySawTx.get()).isFalse();
        assertThat(verifySawTx.get()).isFalse();
    }
}
