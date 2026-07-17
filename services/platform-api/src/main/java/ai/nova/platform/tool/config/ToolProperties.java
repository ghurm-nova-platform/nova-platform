package ai.nova.platform.tool.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.tools")
public class ToolProperties {

    private int maxSchemaCharacters = 20000;
    private int maxInputCharacters = 20000;
    private int maxOutputCharacters = 50000;
    private int maxToolCallsPerExecution = 5;
    private int maxOrchestrationRounds = 5;
    private int defaultTimeoutSeconds = 10;
    private int maximumTimeoutSeconds = 30;
    private boolean enabled = true;

    public int getMaxSchemaCharacters() {
        return maxSchemaCharacters;
    }

    public void setMaxSchemaCharacters(int maxSchemaCharacters) {
        this.maxSchemaCharacters = maxSchemaCharacters;
    }

    public int getMaxInputCharacters() {
        return maxInputCharacters;
    }

    public void setMaxInputCharacters(int maxInputCharacters) {
        this.maxInputCharacters = maxInputCharacters;
    }

    public int getMaxOutputCharacters() {
        return maxOutputCharacters;
    }

    public void setMaxOutputCharacters(int maxOutputCharacters) {
        this.maxOutputCharacters = maxOutputCharacters;
    }

    public int getMaxToolCallsPerExecution() {
        return maxToolCallsPerExecution;
    }

    public void setMaxToolCallsPerExecution(int maxToolCallsPerExecution) {
        this.maxToolCallsPerExecution = maxToolCallsPerExecution;
    }

    public int getMaxOrchestrationRounds() {
        return maxOrchestrationRounds;
    }

    public void setMaxOrchestrationRounds(int maxOrchestrationRounds) {
        this.maxOrchestrationRounds = maxOrchestrationRounds;
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

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
