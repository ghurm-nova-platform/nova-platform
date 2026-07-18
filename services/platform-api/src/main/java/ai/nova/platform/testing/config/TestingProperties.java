package ai.nova.platform.testing.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.testing")
public class TestingProperties {

    private boolean enabled = true;
    private int maxTests = 80;
    private int maxCasesPerTest = 20;
    private int maxArtifactCharsInPrompt = 100000;
    private String defaultProvider = "LOCAL";
    private String defaultModel = "testing-local";
    private String defaultSystemPrompt =
            """
            You are the Nova Testing Agent. Generate structured test plans and test cases as JSON only.
            Never execute Maven, Gradle, npm, Docker, shell, or any code. Never modify repositories.
            Produce testing assets only. Prefer clear unit, API, and negative coverage for critical paths.
            """;
    private String testingStrategy =
            """
            Cover happy paths, validation failures, authorization boundaries, and edge cases.
            Align tests with review findings when present. Prefer deterministic, isolated unit tests first.
            """;
    private String defaultFramework = "JUnit / framework-appropriate for the artifact language";

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxTests() {
        return maxTests;
    }

    public void setMaxTests(int maxTests) {
        this.maxTests = maxTests;
    }

    public int getMaxCasesPerTest() {
        return maxCasesPerTest;
    }

    public void setMaxCasesPerTest(int maxCasesPerTest) {
        this.maxCasesPerTest = maxCasesPerTest;
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

    public String getTestingStrategy() {
        return testingStrategy;
    }

    public void setTestingStrategy(String testingStrategy) {
        this.testingStrategy = testingStrategy;
    }

    public String getDefaultFramework() {
        return defaultFramework;
    }

    public void setDefaultFramework(String defaultFramework) {
        this.defaultFramework = defaultFramework;
    }
}
