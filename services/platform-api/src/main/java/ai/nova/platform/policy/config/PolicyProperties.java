package ai.nova.platform.policy.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.policy")
public class PolicyProperties {

    private boolean enabled = true;
    private boolean allowCustomPolicies = true;
    private boolean retainHistory = true;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowCustomPolicies() {
        return allowCustomPolicies;
    }

    public void setAllowCustomPolicies(boolean allowCustomPolicies) {
        this.allowCustomPolicies = allowCustomPolicies;
    }

    public boolean isRetainHistory() {
        return retainHistory;
    }

    public void setRetainHistory(boolean retainHistory) {
        this.retainHistory = retainHistory;
    }
}
