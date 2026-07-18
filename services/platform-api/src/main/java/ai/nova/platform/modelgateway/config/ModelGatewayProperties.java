package ai.nova.platform.modelgateway.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.model-gateway")
public class ModelGatewayProperties {

    private boolean enabled = true;
    private int maxInputCharacters = 200000;
    private int maxSystemCharacters = 50000;
    private int maxMessageCharacters = 100000;
    private int maxMessages = 200;
    private int maxOutputCharacters = 200000;
    private int maximumOutputTokens = 32768;
    private int defaultTimeoutSeconds = 60;
    private int maximumTimeoutSeconds = 300;
    private int maxProviderAttempts = 5;
    private long maxTotalDurationMs = 300000L;
    private int maxConcurrentRequestsPerProvider = 100;
    private int retryBackoffMaximumMs = 10000;
    private boolean usageEnabled = true;
    private boolean costEstimationEnabled = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxInputCharacters() {
        return maxInputCharacters;
    }

    public void setMaxInputCharacters(int maxInputCharacters) {
        this.maxInputCharacters = maxInputCharacters;
    }

    public int getMaxSystemCharacters() {
        return maxSystemCharacters;
    }

    public void setMaxSystemCharacters(int maxSystemCharacters) {
        this.maxSystemCharacters = maxSystemCharacters;
    }

    public int getMaxMessageCharacters() {
        return maxMessageCharacters;
    }

    public void setMaxMessageCharacters(int maxMessageCharacters) {
        this.maxMessageCharacters = maxMessageCharacters;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }

    public int getMaxOutputCharacters() {
        return maxOutputCharacters;
    }

    public void setMaxOutputCharacters(int maxOutputCharacters) {
        this.maxOutputCharacters = maxOutputCharacters;
    }

    public int getMaximumOutputTokens() {
        return maximumOutputTokens;
    }

    public void setMaximumOutputTokens(int maximumOutputTokens) {
        this.maximumOutputTokens = maximumOutputTokens;
    }

    public int getDefaultTimeoutSeconds() {
        return defaultTimeoutSeconds;
    }

    public void setDefaultTimeoutSeconds(int defaultTimeoutSeconds) {
        this.defaultTimeoutSeconds = defaultTimeoutSeconds;
    }

    public int getMaximumTimeoutSeconds() {
        return maximumTimeoutSeconds;
    }

    public void setMaximumTimeoutSeconds(int maximumTimeoutSeconds) {
        this.maximumTimeoutSeconds = maximumTimeoutSeconds;
    }

    public int getMaxProviderAttempts() {
        return maxProviderAttempts;
    }

    public void setMaxProviderAttempts(int maxProviderAttempts) {
        this.maxProviderAttempts = maxProviderAttempts;
    }

    public long getMaxTotalDurationMs() {
        return maxTotalDurationMs;
    }

    public void setMaxTotalDurationMs(long maxTotalDurationMs) {
        this.maxTotalDurationMs = maxTotalDurationMs;
    }

    public int getMaxConcurrentRequestsPerProvider() {
        return maxConcurrentRequestsPerProvider;
    }

    public void setMaxConcurrentRequestsPerProvider(int maxConcurrentRequestsPerProvider) {
        this.maxConcurrentRequestsPerProvider = maxConcurrentRequestsPerProvider;
    }

    public int getRetryBackoffMaximumMs() {
        return retryBackoffMaximumMs;
    }

    public void setRetryBackoffMaximumMs(int retryBackoffMaximumMs) {
        this.retryBackoffMaximumMs = retryBackoffMaximumMs;
    }

    public boolean isUsageEnabled() {
        return usageEnabled;
    }

    public void setUsageEnabled(boolean usageEnabled) {
        this.usageEnabled = usageEnabled;
    }

    public boolean isCostEstimationEnabled() {
        return costEstimationEnabled;
    }

    public void setCostEstimationEnabled(boolean costEstimationEnabled) {
        this.costEstimationEnabled = costEstimationEnabled;
    }
}
