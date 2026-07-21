package ai.nova.platform.llm.service;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.llm.entity.LlmProviderType;
import ai.nova.platform.llm.entity.LlmUsageMetricEntity;
import ai.nova.platform.llm.repository.LlmUsageMetricRepository;

@Service
public class UsageMetricsService {

    private final LlmUsageMetricRepository repository;
    private final LlmMetrics metrics;

    public UsageMetricsService(LlmUsageMetricRepository repository, LlmMetrics metrics) {
        this.repository = repository;
        this.metrics = metrics;
    }

    @Transactional
    public void record(
            UUID organizationId,
            UUID modelId,
            UUID conversationId,
            LlmProviderType providerType,
            String requestType,
            int inputTokens,
            int outputTokens,
            long inferenceTimeMs,
            boolean success,
            String errorCode) {
        LlmUsageMetricEntity entity = new LlmUsageMetricEntity(
                UUID.randomUUID(),
                organizationId,
                modelId,
                conversationId,
                providerType,
                requestType,
                inputTokens,
                outputTokens,
                inferenceTimeMs,
                success,
                errorCode,
                Instant.now());
        repository.save(entity);
        if (success) {
            metrics.recordSuccess(inferenceTimeMs);
        } else {
            metrics.recordFailure();
        }
    }

    @Transactional(readOnly = true)
    public List<LlmUsageMetricEntity> list(UUID organizationId) {
        return repository.findByOrganizationIdOrderByCreatedAtDesc(organizationId);
    }

    public Map<String, Object> summary() {
        return Map.of(
                "requests", metrics.requestCount(),
                "successes", metrics.successCount(),
                "failures", metrics.failureCount(),
                "fallbacks", metrics.fallbackCount(),
                "lastLatencyMs", metrics.lastLatencyMs());
    }
}
