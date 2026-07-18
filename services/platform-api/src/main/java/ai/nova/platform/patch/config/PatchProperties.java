package ai.nova.platform.patch.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.patch")
public class PatchProperties {

    private boolean enabled = true;
    private int maxPatchChars = 500000;
    private int maxFiles = 200;
    private int maxArtifactCharsInPrompt = 100000;
    private int maxPathLength = 1000;
    private String defaultProvider = "LOCAL";
    private String defaultModel = "patch-local";
    private String defaultSystemPrompt =
            """
            You are the Nova Patch Agent. Generate Git-compatible Unified Diff patches as JSON only.
            Never execute git, shell, apply patches, commit, push, or modify repositories.
            Produce validated patch text only from approved coding artifacts.
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

    public int getMaxPatchChars() {
        return maxPatchChars;
    }

    public void setMaxPatchChars(int maxPatchChars) {
        this.maxPatchChars = maxPatchChars;
    }

    public int getMaxFiles() {
        return maxFiles;
    }

    public void setMaxFiles(int maxFiles) {
        this.maxFiles = maxFiles;
    }

    public int getMaxArtifactCharsInPrompt() {
        return maxArtifactCharsInPrompt;
    }

    public void setMaxArtifactCharsInPrompt(int maxArtifactCharsInPrompt) {
        this.maxArtifactCharsInPrompt = maxArtifactCharsInPrompt;
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

    public String getRepositoryConventions() {
        return repositoryConventions;
    }

    public void setRepositoryConventions(String repositoryConventions) {
        this.repositoryConventions = repositoryConventions;
    }
}
