package ai.nova.platform.rollback.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.rollback")
public class RollbackProperties {

    private boolean enabled = true;
    private boolean executionEnabled = false;
    private boolean allowPlanEdit = false;
    private boolean retainHistory = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isExecutionEnabled() {
        return executionEnabled;
    }

    public void setExecutionEnabled(boolean executionEnabled) {
        this.executionEnabled = executionEnabled;
    }

    public boolean isAllowPlanEdit() {
        return allowPlanEdit;
    }

    public void setAllowPlanEdit(boolean allowPlanEdit) {
        this.allowPlanEdit = allowPlanEdit;
    }

    public boolean isRetainHistory() {
        return retainHistory;
    }

    public void setRetainHistory(boolean retainHistory) {
        this.retainHistory = retainHistory;
    }
}
