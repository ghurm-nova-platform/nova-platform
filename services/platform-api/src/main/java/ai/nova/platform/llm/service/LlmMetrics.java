package ai.nova.platform.llm.service;

import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

@Component
public class LlmMetrics {

    private final Counter requests;
    private final Counter successes;
    private final Counter failures;
    private final Counter fallbacks;
    private final Timer latency;
    private final AtomicLong lastLatencyMs = new AtomicLong();

    public LlmMetrics(MeterRegistry registry) {
        this.requests = registry.counter("nova.llm.requests");
        this.successes = registry.counter("nova.llm.successes");
        this.failures = registry.counter("nova.llm.failures");
        this.fallbacks = registry.counter("nova.llm.fallbacks");
        this.latency = registry.timer("nova.llm.latency");
    }

    public void recordRequest() {
        requests.increment();
    }

    public void recordSuccess(long latencyMs) {
        successes.increment();
        lastLatencyMs.set(latencyMs);
        latency.record(java.time.Duration.ofMillis(Math.max(0, latencyMs)));
    }

    public void recordFailure() {
        failures.increment();
    }

    public void recordFallback() {
        fallbacks.increment();
    }

    public double requestCount() {
        return requests.count();
    }

    public double successCount() {
        return successes.count();
    }

    public double failureCount() {
        return failures.count();
    }

    public double fallbackCount() {
        return fallbacks.count();
    }

    public long lastLatencyMs() {
        return lastLatencyMs.get();
    }
}
