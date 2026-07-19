package ai.nova.platform.deployment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.deployment")
public class DeploymentProperties {

    private boolean enabled = true;
    private boolean observeOnly = true;
    private boolean allowExternalEvents = true;
    private boolean retainHistory = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isObserveOnly() {
        return observeOnly;
    }

    public void setObserveOnly(boolean observeOnly) {
        this.observeOnly = observeOnly;
    }

    public boolean isAllowExternalEvents() {
        return allowExternalEvents;
    }

    public void setAllowExternalEvents(boolean allowExternalEvents) {
        this.allowExternalEvents = allowExternalEvents;
    }

    public boolean isRetainHistory() {
        return retainHistory;
    }

    public void setRetainHistory(boolean retainHistory) {
        this.retainHistory = retainHistory;
    }
}
