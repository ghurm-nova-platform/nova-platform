package ai.nova.platform.review.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.review")
public class ReviewProperties {

    private boolean enabled = true;
    private int maxFindings = 100;
    private int maxArtifactCharsInPrompt = 120000;
    private String defaultProvider = "LOCAL";
    private String defaultModel = "review-local";
    private String defaultSystemPrompt =
            """
            You are the Nova Review Agent. Evaluate generated source artifacts and return JSON only.
            Never modify artifacts, rewrite files, generate patches, execute shell, or edit git.
            Assess correctness, architecture, security, testing, and related quality categories.
            Return a single JSON object matching the documented review schema.
            """;
    private String codingConventions =
            """
            Prefer clear naming, focused modules, validated inputs, and tests for critical paths.
            """;
    private String architectureRules =
            """
            Respect project boundaries. Prefer explicit dependencies and small cohesive units.
            Do not recommend repository mutation or shell execution.
            """;
    private String securityRules =
            """
            Flag injection risks, missing authz checks, secrets in source, unsafe deserialization,
            and insufficient input validation. Prefer least privilege.
            """;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxFindings() {
        return maxFindings;
    }

    public void setMaxFindings(int maxFindings) {
        this.maxFindings = maxFindings;
    }

    public int getMaxArtifactCharsInPrompt() {
        return maxArtifactCharsInPrompt;
    }

    public void setMaxArtifactCharsInPrompt(int maxArtifactCharsInPrompt) {
        this.maxArtifactCharsInPrompt = maxArtifactCharsInPrompt;
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

    public String getArchitectureRules() {
        return architectureRules;
    }

    public void setArchitectureRules(String architectureRules) {
        this.architectureRules = architectureRules;
    }

    public String getSecurityRules() {
        return securityRules;
    }

    public void setSecurityRules(String securityRules) {
        this.securityRules = securityRules;
    }
}
