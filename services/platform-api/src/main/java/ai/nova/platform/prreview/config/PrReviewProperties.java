package ai.nova.platform.prreview.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.pr-review")
public class PrReviewProperties {

    private boolean enabled = true;
    private int maxDiffCharacters = 200000;
    private int defaultLimit = 50;
    private int maxFindings = 200;
    private int maxRecommendations = 100;
    private boolean parallelAnalysis = true;
    private boolean exportEnabled = true;
    private final Cache cache = new Cache();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxDiffCharacters() {
        return maxDiffCharacters;
    }

    public void setMaxDiffCharacters(int maxDiffCharacters) {
        this.maxDiffCharacters = maxDiffCharacters;
    }

    public int getDefaultLimit() {
        return defaultLimit;
    }

    public void setDefaultLimit(int defaultLimit) {
        this.defaultLimit = defaultLimit;
    }

    public int getMaxFindings() {
        return maxFindings;
    }

    public void setMaxFindings(int maxFindings) {
        this.maxFindings = maxFindings;
    }

    public int getMaxRecommendations() {
        return maxRecommendations;
    }

    public void setMaxRecommendations(int maxRecommendations) {
        this.maxRecommendations = maxRecommendations;
    }

    public boolean isParallelAnalysis() {
        return parallelAnalysis;
    }

    public void setParallelAnalysis(boolean parallelAnalysis) {
        this.parallelAnalysis = parallelAnalysis;
    }

    public boolean isExportEnabled() {
        return exportEnabled;
    }

    public void setExportEnabled(boolean exportEnabled) {
        this.exportEnabled = exportEnabled;
    }

    public Cache getCache() {
        return cache;
    }

    public static class Cache {
        private boolean enabled = true;
        private int ttlSeconds = 60;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(int ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }
}
