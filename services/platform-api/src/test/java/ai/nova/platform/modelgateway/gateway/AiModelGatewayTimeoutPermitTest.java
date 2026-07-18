package ai.nova.platform.modelgateway.gateway;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeMessage;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.modelgateway.config.ModelGatewayInvokeExecutorConfig;
import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.entity.AgentModelAssignment;
import ai.nova.platform.modelgateway.entity.AiModel;
import ai.nova.platform.modelgateway.entity.AiModelStatus;
import ai.nova.platform.modelgateway.entity.AiModelType;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderStatus;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.AssignmentRole;
import ai.nova.platform.modelgateway.entity.InvocationStatus;
import ai.nova.platform.modelgateway.entity.ModelInvocation;
import ai.nova.platform.modelgateway.entity.ProjectModel;
import ai.nova.platform.modelgateway.provider.AiModelProvider;
import ai.nova.platform.modelgateway.provider.AiModelProviderRegistry;
import ai.nova.platform.modelgateway.provider.ProviderConcurrencyManager;
import ai.nova.platform.modelgateway.provider.ProviderCredentialResolver;
import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.ProviderInvokeRequest;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.modelgateway.routing.ModelRoutingService;
import ai.nova.platform.modelgateway.routing.ModelRoutingService.ResolvedRouting;
import ai.nova.platform.modelgateway.routing.RoutedModelCandidate;
import ai.nova.platform.modelgateway.usage.ModelUsageRecorder;

class AiModelGatewayTimeoutPermitTest {

    private ModelGatewayProperties properties;
    private ModelRoutingService routingService;
    private ModelInvocationPersistenceService persistenceService;
    private ModelUsageRecorder usageRecorder;
    private AiModelProviderRegistry providerRegistry;
    private ProviderCredentialResolver credentialResolver;
    private ProviderConcurrencyManager concurrencyManager;
    private ModelGatewayRuntimeMapper runtimeMapper;
    private ModelGatewayInputValidator inputValidator;
    private ExecutorService invokeExecutor;
    private AiModelGateway gateway;

    private final UUID providerId = UUID.randomUUID();
    private final UUID executionId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        properties = new ModelGatewayProperties();
        properties.setEnabled(true);
        properties.setMaximumTimeoutSeconds(2);
        properties.setMaxProviderAttempts(1);
        properties.setMaxConcurrentRequestsPerProvider(1);
        properties.setInvokeExecutorPoolSize(2);
        properties.setInvokeExecutorQueueCapacity(2);
        properties.setInvokeCancelGraceMs(3000L);

        routingService = mock(ModelRoutingService.class);
        persistenceService = mock(ModelInvocationPersistenceService.class);
        usageRecorder = mock(ModelUsageRecorder.class);
        providerRegistry = mock(AiModelProviderRegistry.class);
        credentialResolver = mock(ProviderCredentialResolver.class);
        concurrencyManager = new ProviderConcurrencyManager(properties);
        runtimeMapper = mock(ModelGatewayRuntimeMapper.class);
        inputValidator = new ModelGatewayInputValidator(properties);
        invokeExecutor = new ModelGatewayInvokeExecutorConfig().modelGatewayInvokeExecutor(properties);

        gateway = newGateway();

        when(credentialResolver.resolve(nullable(String.class))).thenReturn(Optional.empty());
        when(persistenceService.isExecutionCancelled(any())).thenReturn(false);
        when(persistenceService.nextAttemptNumber(any())).thenReturn(1);
        when(persistenceService.createRunning(
                        any(),
                        any(),
                        any(),
                        any(),
                        any(),
                        nullable(UUID.class),
                        any(),
                        any(),
                        nullable(UUID.class),
                        anyInt(),
                        anyInt(),
                        any(),
                        nullable(UUID.class)))
                .thenAnswer(invocation -> {
                    ModelInvocation mi = new ModelInvocation();
                    mi.setId(invocation.getArgument(0));
                    mi.setExecutionId(executionId);
                    mi.setStatus(InvocationStatus.RUNNING);
                    mi.setStartedAt(Instant.now());
                    return mi;
                });
    }

    @AfterEach
    void tearDown() {
        invokeExecutor.shutdownNow();
    }

    @Test
    void timeoutCancelsFutureAndReleasesPermit() throws Exception {
        CountDownLatch enteredInvoke = new CountDownLatch(1);
        AtomicInteger concurrentInvokes = new AtomicInteger();
        AtomicInteger maxConcurrent = new AtomicInteger();

        AiModelProvider slowProvider = mock(AiModelProvider.class);
        when(slowProvider.adapterKey()).thenReturn("DETERMINISTIC_LOCAL");
        doAnswer(invocation -> {
            int now = concurrentInvokes.incrementAndGet();
            maxConcurrent.updateAndGet(current -> Math.max(current, now));
            enteredInvoke.countDown();
            try {
                Thread.sleep(30_000L);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            } finally {
                concurrentInvokes.decrementAndGet();
            }
            return ProviderInvokeResult.finalResponse("late", 1, 1, 1L, "stop");
        }).when(slowProvider).invoke(any(ProviderInvokeRequest.class));

        RoutedModelCandidate candidate = candidate(1);
        when(routingService.resolve(any())).thenReturn(new ResolvedRouting(null, List.of(candidate)));
        when(providerRegistry.require("DETERMINISTIC_LOCAL")).thenReturn(slowProvider);

        AtomicReference<InvocationStatus> persistedStatus = new AtomicReference<>();
        when(persistenceService.completeFailure(any(), any(), anyString(), anyLong()))
                .thenAnswer(invocation -> {
                    ModelInvocation mi = new ModelInvocation();
                    mi.setId(UUID.randomUUID());
                    mi.setExecutionId(executionId);
                    mi.setStatus(invocation.getArgument(1));
                    mi.setStartedAt(Instant.now());
                    persistedStatus.set(mi.getStatus());
                    return ModelInvocationPersistenceService.CompletionOutcome.from(mi);
                });

        Thread invoker = new Thread(() -> {
            try {
                gateway.invoke(request());
            } catch (RuntimeException ignored) {
                // expected after timeout with no remaining attempts
            }
        });
        invoker.start();

        assertThat(enteredInvoke.await(3, TimeUnit.SECONDS)).isTrue();
        assertThat(concurrencyManager.availablePermits(providerId, 1)).isZero();

        invoker.join(10_000L);
        assertThat(invoker.isAlive()).isFalse();
        assertThat(persistedStatus.get()).isEqualTo(InvocationStatus.TIMED_OUT);
        assertThat(maxConcurrent.get()).isEqualTo(1);

        // Permit free after cancel(true) + worker release — no leak for subsequent acquires.
        assertThat(concurrencyManager.availablePermits(providerId, 1)).isEqualTo(1);
        try (ProviderConcurrencyManager.Permit permit = concurrencyManager.acquire(providerId, 1)) {
            assertThat(permit).isNotNull();
            assertThat(concurrencyManager.availablePermits(providerId, 1)).isZero();
        }
        assertThat(concurrencyManager.availablePermits(providerId, 1)).isEqualTo(1);
    }

    @Test
    void timeoutDoesNotReleasePermitWhileProviderStillRunning() throws Exception {
        CountDownLatch enteredInvoke = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        AtomicInteger activeProviderCalls = new AtomicInteger();

        properties.setInvokeCancelGraceMs(200L);
        gateway = newGateway();

        AiModelProvider stickyProvider = mock(AiModelProvider.class);
        when(stickyProvider.adapterKey()).thenReturn("DETERMINISTIC_LOCAL");
        doAnswer(invocation -> {
            activeProviderCalls.incrementAndGet();
            enteredInvoke.countDown();
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(2);
            while (System.nanoTime() < deadline) {
                try {
                    if (allowFinish.await(50, TimeUnit.MILLISECONDS)) {
                        break;
                    }
                } catch (InterruptedException ex) {
                    Thread.interrupted();
                }
            }
            activeProviderCalls.decrementAndGet();
            return ProviderInvokeResult.finalResponse("late", 1, 1, 1L, "stop");
        }).when(stickyProvider).invoke(any(ProviderInvokeRequest.class));

        RoutedModelCandidate candidate = candidate(1);
        when(routingService.resolve(any())).thenReturn(new ResolvedRouting(null, List.of(candidate)));
        when(providerRegistry.require("DETERMINISTIC_LOCAL")).thenReturn(stickyProvider);
        when(persistenceService.completeFailure(any(), any(), anyString(), anyLong()))
                .thenAnswer(invocation -> {
                    ModelInvocation mi = new ModelInvocation();
                    mi.setId(UUID.randomUUID());
                    mi.setStatus(invocation.getArgument(1));
                    mi.setStartedAt(Instant.now());
                    return ModelInvocationPersistenceService.CompletionOutcome.from(mi);
                });

        Thread invoker = new Thread(() -> {
            try {
                gateway.invoke(request());
            } catch (RuntimeException ignored) {
            }
        });
        invoker.start();
        assertThat(enteredInvoke.await(3, TimeUnit.SECONDS)).isTrue();

        invoker.join(5_000L);
        assertThat(invoker.isAlive()).isFalse();

        if (activeProviderCalls.get() > 0) {
            assertThat(concurrencyManager.availablePermits(providerId, 1)).isZero();
        }

        allowFinish.countDown();
        long waitUntil = System.currentTimeMillis() + 5_000L;
        while (activeProviderCalls.get() > 0 && System.currentTimeMillis() < waitUntil) {
            Thread.sleep(50);
        }
        assertThat(concurrencyManager.availablePermits(providerId, 1)).isEqualTo(1);
    }

    @Test
    void completedStatusFromTx2IsAuthoritative() {
        AiModelProvider fastProvider = mock(AiModelProvider.class);
        when(fastProvider.adapterKey()).thenReturn("DETERMINISTIC_LOCAL");
        try {
            when(fastProvider.invoke(any())).thenReturn(ProviderInvokeResult.finalResponse("ok", 1, 1, 5L, "stop"));
        } catch (ProviderException ex) {
            throw new IllegalStateException(ex);
        }

        RoutedModelCandidate candidate = candidate(1);
        when(routingService.resolve(any())).thenReturn(new ResolvedRouting(null, List.of(candidate)));
        when(providerRegistry.require("DETERMINISTIC_LOCAL")).thenReturn(fastProvider);

        ModelInvocation completed = new ModelInvocation();
        completed.setId(UUID.randomUUID());
        completed.setExecutionId(executionId);
        completed.setStatus(InvocationStatus.COMPLETED);
        completed.setStartedAt(Instant.now());

        when(persistenceService.completeSuccess(any(), any()))
                .thenReturn(ModelInvocationPersistenceService.CompletionOutcome.from(completed));
        when(runtimeMapper.toTurnResult(any(), any()))
                .thenReturn(RuntimeTurnResult.finalResponse(new RuntimeFinalResponse("ok", 1, 1, 2, 5L)));

        ModelGatewayResponse response = gateway.invoke(request());
        assertThat(response.attemptCount()).isEqualTo(1);
        verify(usageRecorder).record(eq(completed), any(), eq(true));
        verify(usageRecorder, never()).record(any(), any(), eq(false));
        verify(persistenceService, never()).completeFailure(any(), any(), anyString(), anyLong());
        // Pre-attempt cancel checks remain; post-TX2 success must not be flipped by a separate cancel read.
        verify(persistenceService, atMost(2)).isExecutionCancelled(executionId);
    }

    private AiModelGateway newGateway() {
        return new AiModelGateway(
                properties,
                routingService,
                persistenceService,
                usageRecorder,
                providerRegistry,
                credentialResolver,
                concurrencyManager,
                runtimeMapper,
                inputValidator,
                invokeExecutor);
    }

    private ModelGatewayRequest request() {
        return new ModelGatewayRequest(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                executionId,
                null,
                UUID.randomUUID(),
                "system",
                List.of(new RuntimeMessage("user", "hello")),
                List.of(),
                List.of(),
                null,
                false,
                false);
    }

    private RoutedModelCandidate candidate(int timeoutSeconds) {
        AiProvider provider = new AiProvider();
        provider.setId(providerId);
        provider.setName("Local");
        provider.setAdapterKey("DETERMINISTIC_LOCAL");
        provider.setProviderType(AiProviderType.DETERMINISTIC_LOCAL);
        provider.setStatus(AiProviderStatus.ACTIVE);
        provider.setRequestTimeoutSeconds(timeoutSeconds);
        provider.setMaxConcurrentRequests(1);
        provider.setMaxRetries(0);
        provider.setRetryBackoffMs(0);

        AiModel model = new AiModel();
        model.setId(UUID.randomUUID());
        model.setDisplayName("det");
        model.setProviderModelId("deterministic-chat-v1");
        model.setModelType(AiModelType.CHAT);
        model.setStatus(AiModelStatus.ACTIVE);
        model.setMaxOutputTokens(1024);
        model.setContextWindowTokens(4096);

        AgentModelAssignment assignment = new AgentModelAssignment();
        assignment.setAssignmentRole(AssignmentRole.PRIMARY);
        assignment.setPriority(1);
        assignment.setEnabled(true);

        ProjectModel projectModel = new ProjectModel();
        projectModel.setEnabled(true);

        return new RoutedModelCandidate(assignment, model, provider, projectModel, false);
    }
}
