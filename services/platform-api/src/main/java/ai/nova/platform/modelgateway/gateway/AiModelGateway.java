package ai.nova.platform.modelgateway.gateway;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.agent.runtime.RuntimeModelMetadata;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.modelgateway.config.ModelGatewayProperties;
import ai.nova.platform.modelgateway.entity.InvocationStatus;
import ai.nova.platform.modelgateway.entity.ModelInvocation;
import ai.nova.platform.modelgateway.entity.ModelRoutingPolicy;
import ai.nova.platform.modelgateway.provider.AiModelProvider;
import ai.nova.platform.modelgateway.provider.AiModelProviderRegistry;
import ai.nova.platform.modelgateway.provider.ProviderConcurrencyManager;
import ai.nova.platform.modelgateway.provider.ProviderCredentialResolver;
import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.ProviderFailureKind;
import ai.nova.platform.modelgateway.provider.ProviderInvokeOutcome;
import ai.nova.platform.modelgateway.provider.ProviderInvokeRequest;
import ai.nova.platform.modelgateway.provider.ProviderInvokeResult;
import ai.nova.platform.modelgateway.routing.ModelRoutingService;
import ai.nova.platform.modelgateway.routing.ModelRoutingService.ResolvedRouting;
import ai.nova.platform.modelgateway.routing.RoutedModelCandidate;
import ai.nova.platform.modelgateway.usage.ModelUsageRecorder;
import ai.nova.platform.web.error.ApiException;

/**
 * Owns provider-neutral model invocation. ToolCallingOrchestrator calls this via GatewayAgentRuntimeClient
 * when nova.model-gateway.enabled=true; knowledge retrieval and tool orchestration remain in orchestrator.
 */
@Service
public class AiModelGateway {

    private static final Logger log = LoggerFactory.getLogger(AiModelGateway.class);

    private final ModelGatewayProperties properties;
    private final ModelRoutingService routingService;
    private final ModelInvocationPersistenceService persistenceService;
    private final ModelUsageRecorder usageRecorder;
    private final AiModelProviderRegistry providerRegistry;
    private final ProviderCredentialResolver credentialResolver;
    private final ProviderConcurrencyManager concurrencyManager;
    private final ModelGatewayRuntimeMapper runtimeMapper;
    private final ModelGatewayInputValidator inputValidator;
    private final ExecutorService invokeExecutor;

    public AiModelGateway(
            ModelGatewayProperties properties,
            ModelRoutingService routingService,
            ModelInvocationPersistenceService persistenceService,
            ModelUsageRecorder usageRecorder,
            AiModelProviderRegistry providerRegistry,
            ProviderCredentialResolver credentialResolver,
            ProviderConcurrencyManager concurrencyManager,
            ModelGatewayRuntimeMapper runtimeMapper,
            ModelGatewayInputValidator inputValidator,
            @Qualifier("modelGatewayInvokeExecutor") ExecutorService invokeExecutor) {
        this.properties = properties;
        this.routingService = routingService;
        this.persistenceService = persistenceService;
        this.usageRecorder = usageRecorder;
        this.providerRegistry = providerRegistry;
        this.credentialResolver = credentialResolver;
        this.concurrencyManager = concurrencyManager;
        this.runtimeMapper = runtimeMapper;
        this.inputValidator = inputValidator;
        this.invokeExecutor = invokeExecutor;
    }

    public ModelGatewayResponse invoke(ModelGatewayRequest request) {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, "MODEL_GATEWAY_DISABLED", "Model gateway disabled");
        }
        inputValidator.validate(request);

        ResolvedRouting routing = routingService.resolve(request);
        ModelRoutingPolicy policy = routing.policy();
        int maxAttempts = resolveMaxAttempts(policy);
        long deadlineMs = System.currentTimeMillis() + resolveMaxDurationMs(policy);

        UUID fallbackFrom = null;
        boolean fallbackUsed = false;
        int attemptCount = 0;

        for (int candidateIndex = 0; candidateIndex < routing.candidates().size(); candidateIndex++) {
            RoutedModelCandidate candidate = routing.candidates().get(candidateIndex);
            if (candidateIndex > 0) {
                fallbackUsed = true;
            }

            AiModelProvider adapter = providerRegistry.require(candidate.provider().getAdapterKey());
            int providerRetries = policy != null && policy.isRetryEnabled()
                    ? Math.min(candidate.provider().getMaxRetries() + 1, maxAttempts - attemptCount)
                    : 1;

            for (int retry = 0; retry < providerRetries && attemptCount < maxAttempts; retry++) {
                if (System.currentTimeMillis() >= deadlineMs) {
                    throw gatewayFailure("MODEL_GATEWAY_TIMEOUT", "Routing deadline exceeded");
                }
                if (persistenceService.isExecutionCancelled(request.executionId())) {
                    throw gatewayFailure("EXECUTION_CANCELLED", "Execution cancelled");
                }

                attemptCount++;
                UUID invocationId = UUID.randomUUID();
                int attemptNumber = persistenceService.nextAttemptNumber(request.executionId());
                persistenceService.createRunning(
                        invocationId,
                        request.organizationId(),
                        request.projectId(),
                        request.agentId(),
                        request.executionId(),
                        request.conversationId(),
                        candidate.provider().getId(),
                        candidate.model().getId(),
                        policy != null ? policy.getId() : null,
                        attemptNumber,
                        inputValidator.countInputCharacters(request),
                        request.createdBy(),
                        fallbackFrom);

                AttemptOutcome outcome = invokeProvider(request, candidate, adapter, invocationId);
                fallbackFrom = invocationId;

                // Usage + success/fallback decisions follow the atomic TX2 status only.
                if (outcome.success()) {
                    RuntimeTurnResult turnResult = runtimeMapper.toTurnResult(
                            outcome.result(), toMetadata(candidate, fallbackUsed, attemptCount));
                    usageRecorder.record(outcome.invocation(), candidate.model(), true);
                    return new ModelGatewayResponse(
                            turnResult,
                            candidate.provider().getId(),
                            candidate.provider().getName(),
                            candidate.model().getId(),
                            candidate.model().getDisplayName(),
                            fallbackUsed,
                            attemptCount);
                }

                usageRecorder.record(outcome.invocation(), candidate.model(), false);
                if (!outcome.retryable()) {
                    break;
                }
                backoff(candidate.provider().getRetryBackoffMs());
            }
        }

        throw gatewayFailure("MODEL_GATEWAY_FAILED", "All model attempts failed");
    }

    private AttemptOutcome invokeProvider(
            ModelGatewayRequest request,
            RoutedModelCandidate candidate,
            AiModelProvider adapter,
            UUID invocationId) {
        long start = System.nanoTime();
        int timeoutSeconds = Math.min(
                candidate.provider().getRequestTimeoutSeconds(),
                properties.getMaximumTimeoutSeconds());

        String credential = credentialResolver
                .resolve(candidate.provider().getCredentialReference())
                .orElse(null);

        int maxOutput = candidate.model().getMaxOutputTokens();
        if (candidate.assignment().getMaximumOutputTokensOverride() != null) {
            maxOutput = candidate.assignment().getMaximumOutputTokensOverride();
        }
        if (candidate.projectModel().getMaximumOutputTokensOverride() != null) {
            maxOutput = candidate.projectModel().getMaximumOutputTokensOverride();
        }
        maxOutput = Math.min(maxOutput, properties.getMaximumOutputTokens());

        ProviderInvokeRequest providerRequest = new ProviderInvokeRequest(
                candidate.model().getProviderModelId(),
                request.systemPrompt(),
                request.messages(),
                request.availableTools(),
                request.toolResults(),
                request.knowledgeContext(),
                maxOutput,
                timeoutSeconds,
                credential);

        UUID providerId = candidate.provider().getId();
        int providerLimit = candidate.provider().getMaxConcurrentRequests();

        // Permit is acquired and released only inside the worker so timeout cannot free capacity
        // while the provider call is still running.
        Callable<ProviderInvokeResult> task = () -> {
            try (ProviderConcurrencyManager.Permit ignored =
                    concurrencyManager.acquire(providerId, providerLimit)) {
                return adapter.invoke(providerRequest);
            }
        };

        Future<ProviderInvokeResult> future;
        try {
            future = invokeExecutor.submit(task);
        } catch (RejectedExecutionException ex) {
            ModelInvocationPersistenceService.CompletionOutcome completion =
                    persistenceService.completeFailure(
                            invocationId,
                            InvocationStatus.RATE_LIMITED,
                            "PROVIDER_UNAVAILABLE",
                            elapsedMs(start));
            return AttemptOutcome.fromCompletion(completion, null, true);
        }

        try {
            ProviderInvokeResult result = future.get(timeoutSeconds, TimeUnit.SECONDS);
            if (result.outcome() == ProviderInvokeOutcome.FAILURE) {
                InvocationStatus status = mapFailureStatus(result.errorCode());
                ModelInvocationPersistenceService.CompletionOutcome completion =
                        persistenceService.completeFailure(
                                invocationId, status, result.errorCode(), elapsedMs(start));
                return AttemptOutcome.fromCompletion(completion, null, isRetryable(result.failureKind()));
            }

            ModelInvocationPersistenceService.CompletionOutcome completion =
                    persistenceService.completeSuccess(invocationId, result);
            return AttemptOutcome.fromCompletion(completion, result, false);
        } catch (TimeoutException ex) {
            future.cancel(true);
            awaitWorkerRelease(future);
            ModelInvocationPersistenceService.CompletionOutcome completion =
                    persistenceService.completeFailure(
                            invocationId, InvocationStatus.TIMED_OUT, "PROVIDER_TIMEOUT", elapsedMs(start));
            return AttemptOutcome.fromCompletion(completion, null, true);
        } catch (CancellationException ex) {
            awaitWorkerRelease(future);
            ModelInvocationPersistenceService.CompletionOutcome completion =
                    persistenceService.completeFailure(
                            invocationId, InvocationStatus.CANCELLED, "PROVIDER_CANCELLED", elapsedMs(start));
            return AttemptOutcome.fromCompletion(completion, null, false);
        } catch (ExecutionException ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof ProviderException providerEx) {
                InvocationStatus status = mapFailureStatus(providerEx.errorCode());
                ModelInvocationPersistenceService.CompletionOutcome completion =
                        persistenceService.completeFailure(
                                invocationId, status, providerEx.errorCode(), elapsedMs(start));
                return AttemptOutcome.fromCompletion(
                        completion, null, providerEx.failureKind() == ProviderFailureKind.TRANSIENT);
            }
            log.warn("Model provider invocation failed (details omitted)");
            ModelInvocationPersistenceService.CompletionOutcome completion =
                    persistenceService.completeFailure(
                            invocationId, InvocationStatus.FAILED, "PROVIDER_ERROR", elapsedMs(start));
            return AttemptOutcome.fromCompletion(completion, null, false);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            future.cancel(true);
            awaitWorkerRelease(future);
            ModelInvocationPersistenceService.CompletionOutcome completion =
                    persistenceService.completeFailure(
                            invocationId, InvocationStatus.CANCELLED, "PROVIDER_CANCELLED", elapsedMs(start));
            return AttemptOutcome.fromCompletion(completion, null, false);
        }
    }

    /**
     * After cancel(true), wait until the worker finishes so the semaphore permit is released before retry.
     */
    private void awaitWorkerRelease(Future<?> future) {
        long graceMs = Math.max(0L, properties.getInvokeCancelGraceMs());
        try {
            future.get(graceMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException ex) {
            log.warn("Provider worker still running after cancel grace; permit remains held until it finishes");
        } catch (CancellationException | ExecutionException ignored) {
            // Worker finished (cancelled or failed) — permit released in worker finally.
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private RuntimeModelMetadata toMetadata(
            RoutedModelCandidate candidate, boolean fallbackUsed, int attemptCount) {
        return new RuntimeModelMetadata(
                candidate.provider().getId(),
                candidate.provider().getName(),
                candidate.model().getId(),
                candidate.model().getDisplayName(),
                fallbackUsed,
                attemptCount);
    }

    private int resolveMaxAttempts(ModelRoutingPolicy policy) {
        int policyAttempts = policy != null ? policy.getMaximumProviderAttempts() : 2;
        return Math.min(Math.max(policyAttempts, 1), properties.getMaxProviderAttempts());
    }

    private long resolveMaxDurationMs(ModelRoutingPolicy policy) {
        long policyDuration = policy != null ? policy.getMaximumTotalDurationMs() : properties.getMaxTotalDurationMs();
        return Math.min(policyDuration, properties.getMaxTotalDurationMs());
    }

    private static InvocationStatus mapFailureStatus(String errorCode) {
        if ("PROVIDER_TIMEOUT".equals(errorCode)) {
            return InvocationStatus.TIMED_OUT;
        }
        if ("PROVIDER_RATE_LIMITED".equals(errorCode)) {
            return InvocationStatus.RATE_LIMITED;
        }
        return InvocationStatus.FAILED;
    }

    private static boolean isRetryable(ProviderFailureKind kind) {
        return kind == ProviderFailureKind.TRANSIENT;
    }

    private static void backoff(int backoffMs) {
        if (backoffMs <= 0) {
            return;
        }
        try {
            Thread.sleep(Math.min(backoffMs, 10_000L));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private static long elapsedMs(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000L;
    }

    private static ApiException gatewayFailure(String code, String message) {
        return new ApiException(HttpStatus.BAD_GATEWAY, code, message);
    }

    private record AttemptOutcome(
            boolean success, boolean retryable, ModelInvocation invocation, ProviderInvokeResult result) {

        static AttemptOutcome fromCompletion(
                ModelInvocationPersistenceService.CompletionOutcome completion,
                ProviderInvokeResult result,
                boolean retryableIfFailed) {
            if (completion.completed()) {
                return new AttemptOutcome(true, false, completion.invocation(), result);
            }
            // CANCELLED and permanent failures must not retry/fallback based on a stale outer check.
            boolean retryable = retryableIfFailed && !completion.cancelled();
            return new AttemptOutcome(false, retryable, completion.invocation(), null);
        }
    }
}
