package ai.nova.platform.orchestration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.orchestration")
public class OrchestrationProperties {

    private boolean enabled = true;
    private long pollIntervalMs = 2000;
    private int claimLimit = 10;
    private int claimLeaseSeconds = 30;
    private int maximumParallelTasks = 20;
    /** Global max CLAIMED+RUNNING tasks across all runs (multi-node enforced via DB). */
    private int globalConcurrency = 20;
    /** Bounded dispatch pool size (must not be unbounded). */
    private int dispatchPoolSize = 20;
    /** Bounded dispatch queue capacity (must not be unbounded). */
    private int dispatchQueueCapacity = 40;
    private int maximumTaskAttempts = 10;
    private int maximumTaskTimeoutSeconds = 600;
    private long maximumRunDurationMs = 3600000;
    private long retryBackoffCapMs = 60000;
    private String workerId = "platform-api";
    private int maxJsonChars = 100000;
    private int maxObjectiveLength = 4000;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public long getPollIntervalMs() {
        return pollIntervalMs;
    }

    public void setPollIntervalMs(long pollIntervalMs) {
        this.pollIntervalMs = pollIntervalMs;
    }

    public int getClaimLimit() {
        return claimLimit;
    }

    public void setClaimLimit(int claimLimit) {
        this.claimLimit = claimLimit;
    }

    public int getClaimLeaseSeconds() {
        return claimLeaseSeconds;
    }

    public void setClaimLeaseSeconds(int claimLeaseSeconds) {
        this.claimLeaseSeconds = claimLeaseSeconds;
    }

    public int getMaximumParallelTasks() {
        return maximumParallelTasks;
    }

    public void setMaximumParallelTasks(int maximumParallelTasks) {
        this.maximumParallelTasks = maximumParallelTasks;
    }

    public int getGlobalConcurrency() {
        return globalConcurrency;
    }

    public void setGlobalConcurrency(int globalConcurrency) {
        this.globalConcurrency = globalConcurrency;
    }

    public int getDispatchPoolSize() {
        return dispatchPoolSize;
    }

    public void setDispatchPoolSize(int dispatchPoolSize) {
        this.dispatchPoolSize = dispatchPoolSize;
    }

    public int getDispatchQueueCapacity() {
        return dispatchQueueCapacity;
    }

    public void setDispatchQueueCapacity(int dispatchQueueCapacity) {
        this.dispatchQueueCapacity = dispatchQueueCapacity;
    }

    public int getMaximumTaskAttempts() {
        return maximumTaskAttempts;
    }

    public void setMaximumTaskAttempts(int maximumTaskAttempts) {
        this.maximumTaskAttempts = maximumTaskAttempts;
    }

    public int getMaximumTaskTimeoutSeconds() {
        return maximumTaskTimeoutSeconds;
    }

    public void setMaximumTaskTimeoutSeconds(int maximumTaskTimeoutSeconds) {
        this.maximumTaskTimeoutSeconds = maximumTaskTimeoutSeconds;
    }

    public long getMaximumRunDurationMs() {
        return maximumRunDurationMs;
    }

    public void setMaximumRunDurationMs(long maximumRunDurationMs) {
        this.maximumRunDurationMs = maximumRunDurationMs;
    }

    public long getRetryBackoffCapMs() {
        return retryBackoffCapMs;
    }

    public void setRetryBackoffCapMs(long retryBackoffCapMs) {
        this.retryBackoffCapMs = retryBackoffCapMs;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public int getMaxJsonChars() {
        return maxJsonChars;
    }

    public void setMaxJsonChars(int maxJsonChars) {
        this.maxJsonChars = maxJsonChars;
    }

    public int getMaxObjectiveLength() {
        return maxObjectiveLength;
    }

    public void setMaxObjectiveLength(int maxObjectiveLength) {
        this.maxObjectiveLength = maxObjectiveLength;
    }
}
