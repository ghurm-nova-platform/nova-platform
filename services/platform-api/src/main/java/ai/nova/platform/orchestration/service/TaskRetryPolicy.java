package ai.nova.platform.orchestration.service;

import java.util.Set;

import org.springframework.stereotype.Component;

import ai.nova.platform.orchestration.config.OrchestrationProperties;

@Component
public class TaskRetryPolicy {

    private static final Set<String> NON_RETRYABLE = Set.of(
            "INVALID_INPUT",
            "FORBIDDEN",
            "UNAUTHORIZED",
            "AUTHENTICATION_FAILED",
            "MODEL_REFERENCE_INVALID",
            "MODEL_REFERENCE_NOT_FOUND",
            "ORCHESTRATION_AGENT_REQUIRED",
            "ORCHESTRATION_MODEL_REFERENCE_INVALID",
            "ORCHESTRATION_CAPABILITY_INVALID",
            "ORCHESTRATION_GRAPH_CYCLE",
            "TASK_TYPE_UNSUPPORTED",
            "EXECUTION_CANCELLED",
            "CANCELLED");

    private final OrchestrationProperties properties;

    public TaskRetryPolicy(OrchestrationProperties properties) {
        this.properties = properties;
    }

    public boolean isRetryable(String errorCode) {
        if (errorCode == null || errorCode.isBlank()) {
            return true;
        }
        return !NON_RETRYABLE.contains(errorCode);
    }

    public long nextBackoffMs(int attemptNumber, long baseBackoffMs) {
        long base = Math.max(0, baseBackoffMs);
        long exp = base * (1L << Math.min(Math.max(attemptNumber - 1, 0), 16));
        return Math.min(exp, properties.getRetryBackoffCapMs());
    }
}
