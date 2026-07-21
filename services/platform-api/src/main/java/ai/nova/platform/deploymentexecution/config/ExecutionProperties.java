package ai.nova.platform.deploymentexecution.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.execution")
public class ExecutionProperties {

    private boolean enabled = true;
    private String provider = "LOCAL";
    private int retryCount = 0;
    private int verificationTimeoutSeconds = 60;
    private boolean allowCancel = true;
    private String restBaseUrl = "";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getRetryCount() {
        return retryCount;
    }

    public void setRetryCount(int retryCount) {
        this.retryCount = retryCount;
    }

    public int getVerificationTimeoutSeconds() {
        return verificationTimeoutSeconds;
    }

    public void setVerificationTimeoutSeconds(int verificationTimeoutSeconds) {
        this.verificationTimeoutSeconds = verificationTimeoutSeconds;
    }

    public boolean isAllowCancel() {
        return allowCancel;
    }

    public void setAllowCancel(boolean allowCancel) {
        this.allowCancel = allowCancel;
    }

    public String getRestBaseUrl() {
        return restBaseUrl;
    }

    public void setRestBaseUrl(String restBaseUrl) {
        this.restBaseUrl = restBaseUrl;
    }
}
