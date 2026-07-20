package ai.nova.platform.environment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.environment")
public class EnvironmentProperties {

    private boolean enabled = true;
    private boolean allowDelete = false;
    private boolean retainHistory = true;
    private boolean allowMultipleProduction = false;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowDelete() {
        return allowDelete;
    }

    public void setAllowDelete(boolean allowDelete) {
        this.allowDelete = allowDelete;
    }

    public boolean isRetainHistory() {
        return retainHistory;
    }

    public void setRetainHistory(boolean retainHistory) {
        this.retainHistory = retainHistory;
    }

    public boolean isAllowMultipleProduction() {
        return allowMultipleProduction;
    }

    public void setAllowMultipleProduction(boolean allowMultipleProduction) {
        this.allowMultipleProduction = allowMultipleProduction;
    }
}
