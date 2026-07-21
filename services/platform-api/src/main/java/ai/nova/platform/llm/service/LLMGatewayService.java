package ai.nova.platform.llm.service;

import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import ai.nova.platform.llm.configuration.LlmProperties;
import ai.nova.platform.llm.entity.LlmModelEntity;
import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.error.LlmErrorCodes;
import ai.nova.platform.llm.gateway.LLMGateway;
import ai.nova.platform.llm.provider.LlmCompletionRequest;
import ai.nova.platform.llm.provider.LlmCompletionResult;
import ai.nova.platform.llm.provider.LlmProviderException;
import ai.nova.platform.llm.provider.LlmProviderRegistry;
import ai.nova.platform.llm.provider.LlmRuntimeProvider;
import ai.nova.platform.llm.repository.LlmModelRepository;
import ai.nova.platform.web.error.ApiException;

import jakarta.annotation.PreDestroy;

@Service
public class LLMGatewayService implements LLMGateway {

    private final LlmProperties properties;
    private final LlmProviderRegistry providerRegistry;
    private final LlmModelRepository modelRepository;
    private final LlmCacheService cacheService;
    private final UsageMetricsService usageMetricsService;
    private final LlmMetrics metrics;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    public LLMGatewayService(
            LlmProperties properties,
            LlmProviderRegistry providerRegistry,
            LlmModelRepository modelRepository,
            LlmCacheService cacheService,
            UsageMetricsService usageMetricsService,
            LlmMetrics metrics) {
        this.properties = properties;
        this.providerRegistry = providerRegistry;
        this.modelRepository = modelRepository;
        this.cacheService = cacheService;
        this.usageMetricsService = usageMetricsService;
        this.metrics = metrics;
    }

    @PreDestroy
    void shutdown() {
        executor.shutdownNow();
    }

    @Override
    public LlmCompletionResult complete(UUID organizationId, LlmModelEntity model, LlmCompletionRequest request) {
        if (!properties.isEnabled()) {
            throw new ApiException(HttpStatus.SERVICE_UNAVAILABLE, LlmErrorCodes.DISABLED, "Local LLM runtime disabled");
        }
        metrics.recordRequest();
        String cacheKey = cacheKey(organizationId, model.getCode(), request);
        return cacheService
                .get(cacheKey)
                .orElseGet(() -> {
                    LlmCompletionResult result = invokeWithRetryAndFallback(organizationId, model, request);
                    cacheService.put(cacheKey, result);
                    return result;
                });
    }

    @Override
    public LlmCompletionResult completeByModelCode(
            UUID organizationId, String modelCode, LlmCompletionRequest request) {
        LlmModelEntity model = modelRepository
                .findByOrganizationIdAndCode(organizationId, modelCode)
                .orElseThrow(() -> new ApiException(
                        HttpStatus.NOT_FOUND, LlmErrorCodes.NOT_FOUND, "Model not found: " + modelCode));
        LlmCompletionRequest effective = new LlmCompletionRequest(
                model.getCode(),
                request.messages(),
                request.maxTokens(),
                request.temperature(),
                request.stream());
        return complete(organizationId, model, effective);
    }

    private LlmCompletionResult invokeWithRetryAndFallback(
            UUID organizationId, LlmModelEntity model, LlmCompletionRequest request) {
        LlmProviderType preferred = model.getProviderType();
        int maxAttempts = Math.max(1, properties.getRetry().getMaxAttempts());
        LlmProviderException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                LlmCompletionResult result = invokeTimed(preferred, request);
                usageMetricsService.record(
                        organizationId,
                        model.getId(),
                        null,
                        result.providerType(),
                        "COMPLETION",
                        result.inputTokens(),
                        result.outputTokens(),
                        result.latencyMs(),
                        true,
                        null);
                return result;
            } catch (LlmProviderException ex) {
                lastError = ex;
                if (!ex.isRetryable() || attempt >= maxAttempts) {
                    break;
                }
                sleep(properties.getRetry().getBackoffMs());
            } catch (Exception ex) {
                lastError = new LlmProviderException(LlmErrorCodes.PROVIDER_ERROR, ex.getMessage(), true);
                if (attempt >= maxAttempts) {
                    break;
                }
                sleep(properties.getRetry().getBackoffMs());
            }
        }

        if (properties.isFallbackToDeterministic() && preferred != LlmProviderType.DETERMINISTIC) {
            metrics.recordFallback();
            try {
                LlmCompletionResult result = invokeTimed(LlmProviderType.DETERMINISTIC, request);
                usageMetricsService.record(
                        organizationId,
                        model.getId(),
                        null,
                        result.providerType(),
                        "COMPLETION_FALLBACK",
                        result.inputTokens(),
                        result.outputTokens(),
                        result.latencyMs(),
                        true,
                        null);
                return result;
            } catch (Exception fallbackEx) {
                usageMetricsService.record(
                        organizationId,
                        model.getId(),
                        null,
                        preferred,
                        "COMPLETION",
                        0,
                        0,
                        0,
                        false,
                        lastError != null ? lastError.getErrorCode() : LlmErrorCodes.PROVIDER_ERROR);
                throw new ApiException(
                        HttpStatus.BAD_GATEWAY,
                        LlmErrorCodes.PROVIDER_ERROR,
                        "LLM provider failed: " + fallbackEx.getMessage());
            }
        }

        usageMetricsService.record(
                organizationId,
                model.getId(),
                null,
                preferred,
                "COMPLETION",
                0,
                0,
                0,
                false,
                lastError != null ? lastError.getErrorCode() : LlmErrorCodes.PROVIDER_ERROR);
        throw new ApiException(
                HttpStatus.BAD_GATEWAY,
                lastError != null ? lastError.getErrorCode() : LlmErrorCodes.PROVIDER_ERROR,
                lastError != null ? lastError.getMessage() : "LLM provider failed");
    }

    private LlmCompletionResult invokeTimed(LlmProviderType type, LlmCompletionRequest request) {
        LlmRuntimeProvider provider = providerRegistry.require(type);
        int timeoutSeconds = Math.max(1, properties.getTimeout().getSeconds());
        Callable<LlmCompletionResult> task = () -> provider.complete(request);
        Future<LlmCompletionResult> future = executor.submit(task);
        try {
            return future.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException ex) {
            future.cancel(true);
            throw new LlmProviderException(LlmErrorCodes.PROVIDER_ERROR, "LLM provider timeout", true);
        } catch (Exception ex) {
            Throwable cause = ex.getCause() != null ? ex.getCause() : ex;
            if (cause instanceof LlmProviderException providerEx) {
                throw providerEx;
            }
            throw new LlmProviderException(LlmErrorCodes.PROVIDER_ERROR, cause.getMessage(), true);
        }
    }

    private static String cacheKey(UUID organizationId, String modelCode, LlmCompletionRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append(organizationId).append('|').append(modelCode).append('|');
        if (request.messages() != null) {
            for (var message : request.messages()) {
                sb.append(message.role()).append(':').append(message.content()).append(';');
            }
        }
        sb.append('|').append(request.maxTokens()).append('|').append(request.temperature());
        return sb.toString();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(Math.max(0, ms));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }
}
