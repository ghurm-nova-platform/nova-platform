package ai.nova.platform.audit.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.audit")
public class AuditProperties {

    private boolean enabled = true;
    private boolean immutable = true;
    private boolean retainHistory = true;
    private boolean captureRestApi = true;
    private boolean captureSecurityEvents = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isImmutable() {
        return immutable;
    }

    public void setImmutable(boolean immutable) {
        this.immutable = immutable;
    }

    public boolean isRetainHistory() {
        return retainHistory;
    }

    public void setRetainHistory(boolean retainHistory) {
        this.retainHistory = retainHistory;
    }

    public boolean isCaptureRestApi() {
        return captureRestApi;
    }

    public void setCaptureRestApi(boolean captureRestApi) {
        this.captureRestApi = captureRestApi;
    }

    public boolean isCaptureSecurityEvents() {
        return captureSecurityEvents;
    }

    public void setCaptureSecurityEvents(boolean captureSecurityEvents) {
        this.captureSecurityEvents = captureSecurityEvents;
    }
}
