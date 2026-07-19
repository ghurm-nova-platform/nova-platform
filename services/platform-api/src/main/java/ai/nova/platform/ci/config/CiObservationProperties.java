package ai.nova.platform.ci.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.ci-observation")
public class CiObservationProperties {

    private boolean enabled = false;
    private String provider = "GITHUB";
    private int requestTimeoutSeconds = 30;
    private String githubApiBaseUrl = "https://api.github.com";
    private String githubToken = "";
    private int maxRunsPerObservation = 20;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public int getRequestTimeoutSeconds() {
        return requestTimeoutSeconds;
    }

    public void setRequestTimeoutSeconds(int requestTimeoutSeconds) {
        this.requestTimeoutSeconds = requestTimeoutSeconds;
    }

    public String getGithubApiBaseUrl() {
        return githubApiBaseUrl;
    }

    public void setGithubApiBaseUrl(String githubApiBaseUrl) {
        this.githubApiBaseUrl = githubApiBaseUrl;
    }

    public String getGithubToken() {
        return githubToken;
    }

    public void setGithubToken(String githubToken) {
        this.githubToken = githubToken;
    }

    public int getMaxRunsPerObservation() {
        return maxRunsPerObservation;
    }

    public void setMaxRunsPerObservation(int maxRunsPerObservation) {
        this.maxRunsPerObservation = maxRunsPerObservation;
    }
}
