package ai.nova.platform.prompt.validation;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.prompts")
public class PromptProperties {

    private int maxContentLength = 50000;
    private int maxTags = 20;
    private int maxTagLength = 100;

    public int getMaxContentLength() {
        return maxContentLength;
    }

    public void setMaxContentLength(int maxContentLength) {
        this.maxContentLength = maxContentLength;
    }

    public int getMaxTags() {
        return maxTags;
    }

    public void setMaxTags(int maxTags) {
        this.maxTags = maxTags;
    }

    public int getMaxTagLength() {
        return maxTagLength;
    }

    public void setMaxTagLength(int maxTagLength) {
        this.maxTagLength = maxTagLength;
    }
}
