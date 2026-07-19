package ai.nova.platform.repair.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.repair")
public class RepairProperties {

    private boolean enabled = false;
    private int maxAttempts = 5;
    private int maxFiles = 20;
    private int maxGeneratedLines = 1000;
    private boolean allowPartialRepair = true;
    private boolean requireReviewBeforeRepair = false;
    private String defaultProvider = "LOCAL";
    private String defaultModel = "repair-local";
    private String defaultSystemPrompt =
            """
            You are the Nova Repair Agent. Propose Git-compatible Unified Diff patches as JSON only to fix \
            failures from review, testing, or CI observation.
            Never execute git, shell, apply patches, commit, push, merge, approve, deploy, or modify prior patch rows.
            Produce a NEW validated patch that addresses the collected failure inputs.
            """;
    private String repositoryConventions =
            """
            Use relative repository paths. Prefer --- a/<path> and +++ b/<path> headers.
            Include complete hunks with @@ -old,count +new,count @@ markers.
            Do not include binary files or absolute paths.
            """;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public int getMaxGeneratedLines() {
        return maxGeneratedLines;
    }

    public void setMaxGeneratedLines(int maxGeneratedLines) {
        this.maxGeneratedLines = maxGeneratedLines;
    }

    public boolean isAllowPartialRepair() {
        return allowPartialRepair;
    }

    public void setAllowPartialRepair(boolean allowPartialRepair) {
        this.allowPartialRepair = allowPartialRepair;
    }

    public boolean isRequireReviewBeforeRepair() {
        return requireReviewBeforeRepair;
    }

    public void setRequireReviewBeforeRepair(boolean requireReviewBeforeRepair) {
        this.requireReviewBeforeRepair = requireReviewBeforeRepair;
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

    public String getRepositoryConventions() {
        return repositoryConventions;
    }

    public void setRepositoryConventions(String repositoryConventions) {
        this.repositoryConventions = repositoryConventions;
    }
}
