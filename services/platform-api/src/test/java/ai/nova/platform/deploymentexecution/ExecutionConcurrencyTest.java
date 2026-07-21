package ai.nova.platform.deploymentexecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.EnumSet;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deploymentexecution.dto.ExecutionDtos.CreateExecutionRequest;
import ai.nova.platform.deploymentexecution.entity.ExecutionProviderCode;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.repository.DeploymentExecutionRepository;
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

    private static final EnumSet<ExecutionStatus> ACTIVE = EnumSet.of(
            ExecutionStatus.READY,
            ExecutionStatus.QUEUED,
            ExecutionStatus.STARTING,
            ExecutionStatus.DEPLOYING,
            ExecutionStatus.VERIFYING);

    @Autowired
    private DeploymentExecutionService deploymentExecutionService;

    @Autowired
    private DeploymentExecutionRepository executionRepository;

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

        long patch = Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
        UUID otherRelease = seedSupport.seedPublishedRelease("82.9." + patch);
        UUID otherDeployment = seedSupport.observeDeployment(null, null, otherRelease);
        seedSupport.seedReadyRollback(otherRelease, otherDeployment, ctx.targetReleaseId());

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

    @Test
    void concurrentCreatesAllowExactlyOneActiveExecution() throws Exception {
        var ctxA = seedSupport.seedExecutionReadyContext();
        long patch = Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
        UUID releaseB = seedSupport.seedPublishedRelease("82.8." + patch);
        UUID deploymentB = seedSupport.observeDeployment(null, null, releaseB);
        seedSupport.seedReadyRollback(releaseB, deploymentB, ctxA.targetReleaseId());

        CountDownLatch startGate = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        AtomicReference<Throwable> errorA = new AtomicReference<>();
        AtomicReference<Throwable> errorB = new AtomicReference<>();
        AtomicReference<UUID> idA = new AtomicReference<>();
        AtomicReference<UUID> idB = new AtomicReference<>();

        Future<?> f1 = pool.submit(() -> {
            try {
                startGate.await(5, TimeUnit.SECONDS);
                idA.set(deploymentExecutionService
                        .create(
                                new CreateExecutionRequest(
                                        ctxA.releaseId(),
                                        ExecutionTestFixture.STAGING_ENVIRONMENT_ID,
                                        null,
                                        ExecutionProviderCode.LOCAL,
                                        null),
                                user)
                        .id());
            } catch (Throwable t) {
                errorA.set(t);
            }
        });
        Future<?> f2 = pool.submit(() -> {
            try {
                startGate.await(5, TimeUnit.SECONDS);
                idB.set(deploymentExecutionService
                        .create(
                                new CreateExecutionRequest(
                                        releaseB,
                                        ExecutionTestFixture.STAGING_ENVIRONMENT_ID,
                                        null,
                                        ExecutionProviderCode.LOCAL,
                                        null),
                                user)
                        .id());
            } catch (Throwable t) {
                errorB.set(t);
            }
        });
        startGate.countDown();
        f1.get(20, TimeUnit.SECONDS);
        f2.get(20, TimeUnit.SECONDS);
        pool.shutdownNow();

        int successes = (errorA.get() == null ? 1 : 0) + (errorB.get() == null ? 1 : 0);
        int blocked = 0;
        for (Throwable t : new Throwable[] {errorA.get(), errorB.get()}) {
            if (t instanceof ApiException api && "EXECUTION_CONCURRENCY_BLOCKED".equals(api.getCode())) {
                blocked++;
            } else if (t != null) {
                throw new AssertionError("Unexpected failure", t);
            }
        }
        assertThat(successes).isEqualTo(1);
        assertThat(blocked).isEqualTo(1);
        assertThat(executionRepository.countByOrganizationIdAndEnvironmentIdAndStatusIn(
                        ExecutionTestFixture.ORG_ID, ExecutionTestFixture.STAGING_ENVIRONMENT_ID, ACTIVE))
                .isEqualTo(1);
    }

    @Test
    void allowsNewExecutionAfterCompletedFailedOrCancelled() throws Exception {
        var ctx1 = seedSupport.seedExecutionReadyContext();
        var created = deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx1.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);
        deploymentExecutionService.start(created.id(), user);
        ExecutionTestFixture.awaitStatus(
                () -> deploymentExecutionService.get(created.id(), user).status(),
                ExecutionStatus.COMPLETED,
                10_000);

        var ctx2 = seedSupport.seedExecutionReadyContext();
        var second = deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx2.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);
        assertThat(second.status()).isEqualTo(ExecutionStatus.QUEUED);
        deploymentExecutionService.cancel(second.id(), user);
        assertThat(deploymentExecutionService.get(second.id(), user).status()).isEqualTo(ExecutionStatus.CANCELLED);

        var ctx3 = seedSupport.seedExecutionReadyContext();
        var third = deploymentExecutionService.create(
                new CreateExecutionRequest(
                        ctx3.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, null, ExecutionProviderCode.LOCAL, null),
                user);
        assertThat(third.status()).isEqualTo(ExecutionStatus.QUEUED);
    }
}
