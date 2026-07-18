package ai.nova.platform.coding.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.coding")
public class CodingProperties {

    private boolean enabled = true;
    private int maxArtifacts = 40;
    private int maxContentChars = 200000;
    private int maxPathLength = 1000;
    private String defaultProvider = "LOCAL";
    private String defaultModel = "coding-local";
    private String defaultSystemPrompt =
            """
            You are the Nova Coding Agent. Generate source code artifacts as JSON only.
            Never execute shell, git, docker, browser, MCP, or terminal commands.
            Never modify repositories. Produce structured artifacts only.
            Return a single JSON object with summary and artifacts matching the schema.
            """;
    private String codingConventions =
            """
            Prefer clear naming, small focused files, and tests alongside source when appropriate.
            Use relative repository paths. Do not invent absolute filesystem paths.
            """;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxArtifacts() {
        return maxArtifacts;
    }

    public void setMaxArtifacts(int maxArtifacts) {
        this.maxArtifacts = maxArtifacts;
    }

    public int getMaxContentChars() {
        return maxContentChars;
    }

    public void setMaxContentChars(int maxContentChars) {
        this.maxContentChars = maxContentChars;
    }

    public int getMaxPathLength() {
        return maxPathLength;
    }

    public void setMaxPathLength(int maxPathLength) {
        this.maxPathLength = maxPathLength;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public String getDefaultModel() {
        return defaultModel;
    }

    public void setDefaultModel(String defaultModel) {
        this.defaultModel = defaultModel;
    }

    public String getDefaultSystemPrompt() {
        return defaultSystemPrompt;
    }

    public void setDefaultSystemPrompt(String defaultSystemPrompt) {
        this.defaultSystemPrompt = defaultSystemPrompt;
    }

    public String getCodingConventions() {
        return codingConventions;
    }

    public void setCodingConventions(String codingConventions) {
        this.codingConventions = codingConventions;
    }
}
