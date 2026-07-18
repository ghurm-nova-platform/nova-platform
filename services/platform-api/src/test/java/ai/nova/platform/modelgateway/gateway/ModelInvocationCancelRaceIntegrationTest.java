package ai.nova.platform.modelgateway.gateway;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.repository.AgentExecutionRepository;
import ai.nova.platform.modelgateway.entity.InvocationStatus;
import ai.nova.platform.modelgateway.entity.ModelInvocation;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.modelgateway.repository.AiModelRepository;
import ai.nova.platform.modelgateway.repository.ModelInvocationRepository;
import ai.nova.platform.modelgateway.usage.ModelUsageRecorder;

@SpringBootTest
class ModelInvocationCancelRaceIntegrationTest {

    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID PROJECT_ID = UUID.fromString("55555555-5555-5555-5555-555555555501");
    private static final UUID AGENT_ID = UUID.fromString("66666666-6666-6666-6666-666666666601");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final UUID PROVIDER_ID = UUID.fromString("99999999-9999-9999-9999-999999999901");
    private static final UUID MODEL_ID = UUID.fromString("99999999-9999-9999-9999-999999999911");
    private static final UUID PROMPT_VERSION_ID = UUID.fromString("66666666-6666-6666-6666-666666666611");

    @Autowired
    private ModelInvocationPersistenceService persistenceService;
    @Autowired
    private AgentExecutionRepository executionRepository;
    @Autowired
    private ModelInvocationRepository invocationRepository;
    @Autowired
    private ModelUsageRecorder usageRecorder;
    @Autowired
    private TransactionTemplate transactionTemplate;
    @Autowired
    private AiModelRepository aiModelRepository;

    @Test
    void completeSuccessAfterCancelPersistsCancelledAtomically() {
        UUID executionId = UUID.randomUUID();
        UUID invocationId = UUID.randomUUID();
        seedRunning(executionId, invocationId);

        transactionTemplate.executeWithoutResult(status -> {
            AgentExecution locked = executionRepository.findByIdForUpdate(executionId).orElseThrow();
            locked.setStatus(ExecutionStatus.CANCELLED);
            locked.setCompletedAt(Instant.now());
            executionRepository.save(locked);
        });

        ProviderInvokeResult result =
                ProviderInvokeResult.finalResponse("should-not-count-as-success", 3, 2, 10L, "stop");
        ModelInvocationPersistenceService.CompletionOutcome outcome =
                persistenceService.completeSuccess(invocationId, result);

        assertThat(outcome.cancelled()).isTrue();
        assertThat(outcome.completed()).isFalse();
        assertThat(outcome.status()).isEqualTo(InvocationStatus.CANCELLED);

        ModelInvocation stored = invocationRepository.findById(invocationId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(InvocationStatus.CANCELLED);
        assertThat(stored.getOutputCharacterCount()).isNull();

        var model = aiModelRepository.findById(MODEL_ID).orElseThrow();
        usageRecorder.record(stored, model, outcome.completed());
        assertThat(ModelUsageRecorder.isSuccessful(stored.getStatus())).isFalse();
    }

    @Test
    void concurrentCancelDuringCompleteSuccessYieldsSingleAtomicStatus() throws Exception {
        UUID executionId = UUID.randomUUID();
        UUID invocationId = UUID.randomUUID();
        seedRunning(executionId, invocationId);

        CountDownLatch bothReady = new CountDownLatch(2);
        CountDownLatch start = new CountDownLatch(1);
        AtomicReference<InvocationStatus> completionStatus = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> completeFuture = pool.submit(() -> {
                bothReady.countDown();
                start.await(5, TimeUnit.SECONDS);
                ProviderInvokeResult result = ProviderInvokeResult.finalResponse("race", 1, 1, 5L, "stop");
                ModelInvocationPersistenceService.CompletionOutcome outcome =
                        persistenceService.completeSuccess(invocationId, result);
                completionStatus.set(outcome.status());
                return null;
            });

            Future<?> cancelFuture = pool.submit(() -> {
                bothReady.countDown();
                start.await(5, TimeUnit.SECONDS);
                transactionTemplate.executeWithoutResult(status -> {
                    AgentExecution locked = executionRepository.findByIdForUpdate(executionId).orElseThrow();
                    if (locked.getStatus() == ExecutionStatus.RUNNING) {
                        locked.setStatus(ExecutionStatus.CANCELLED);
                        locked.setCompletedAt(Instant.now());
                        executionRepository.save(locked);
                    }
                });
                return null;
            });

            assertThat(bothReady.await(5, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            completeFuture.get(10, TimeUnit.SECONDS);
            cancelFuture.get(10, TimeUnit.SECONDS);

            ModelInvocation stored = invocationRepository.findById(invocationId).orElseThrow();
            assertThat(stored.getStatus()).isIn(InvocationStatus.COMPLETED, InvocationStatus.CANCELLED);
            assertThat(completionStatus.get()).isEqualTo(stored.getStatus());

            var model = aiModelRepository.findById(MODEL_ID).orElseThrow();
            boolean success = stored.getStatus() == InvocationStatus.COMPLETED;
            usageRecorder.record(stored, model, success);
            assertThat(ModelUsageRecorder.isSuccessful(stored.getStatus())).isEqualTo(success);
        } finally {
            start.countDown();
            pool.shutdownNow();
        }
    }

    private void seedRunning(UUID executionId, UUID invocationId) {
        executionRepository.saveAndFlush(new AgentExecution(
                executionId,
                ORG_ID,
                PROJECT_ID,
                AGENT_ID,
                PROMPT_VERSION_ID,
                null,
                "DETERMINISTIC_LOCAL",
                "deterministic-chat-v1",
                ExecutionStatus.RUNNING,
                USER_ID,
                Instant.now()));

        persistenceService.createRunning(
                invocationId,
                ORG_ID,
                PROJECT_ID,
                AGENT_ID,
                executionId,
                null,
                PROVIDER_ID,
                MODEL_ID,
                null,
                1,
                8,
                USER_ID,
                null);
    }
}
