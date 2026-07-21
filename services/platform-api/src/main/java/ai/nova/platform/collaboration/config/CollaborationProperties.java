package ai.nova.platform.collaboration.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.collaboration")
public class CollaborationProperties {

    private boolean enabled = true;
    private int pollingSeconds = 10;
    private int maxMessages = 500;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getPollingSeconds() {
        return pollingSeconds;
    }

    public void setPollingSeconds(int pollingSeconds) {
        this.pollingSeconds = pollingSeconds;
    }

    public int getMaxMessages() {
        return maxMessages;
    }

    public void setMaxMessages(int maxMessages) {
        this.maxMessages = maxMessages;
    }
}
