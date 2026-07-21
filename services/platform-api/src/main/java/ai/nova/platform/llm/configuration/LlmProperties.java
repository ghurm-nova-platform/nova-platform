package ai.nova.platform.llm.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "nova.llm")
public class LlmProperties {

    private boolean enabled = true;
    private String defaultProvider = "DETERMINISTIC";
    private boolean fallbackToDeterministic = true;
    private final Timeout timeout = new Timeout();
    private final Retry retry = new Retry();
    private final Cache cache = new Cache();
    private final Scheduler scheduler = new Scheduler();
    private final ProviderEndpoint ollama = new ProviderEndpoint();
    private final ProviderEndpoint llamacpp = new ProviderEndpoint();
    private final ProviderEndpoint vllm = new ProviderEndpoint();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public String getDefaultProvider() {
        return defaultProvider;
    }

    public void setDefaultProvider(String defaultProvider) {
        this.defaultProvider = defaultProvider;
    }

    public boolean isFallbackToDeterministic() {
        return fallbackToDeterministic;
    }

    public void setFallbackToDeterministic(boolean fallbackToDeterministic) {
        this.fallbackToDeterministic = fallbackToDeterministic;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public Retry getRetry() {
        return retry;
    }

    public Cache getCache() {
        return cache;
    }

    public Scheduler getScheduler() {
        return scheduler;
    }

    public ProviderEndpoint getOllama() {
        return ollama;
    }

    public ProviderEndpoint getLlamacpp() {
        return llamacpp;
    }

    public ProviderEndpoint getVllm() {
        return vllm;
    }

    public static class Timeout {
        private int seconds = 60;

        public int getSeconds() {
            return seconds;
        }

        public void setSeconds(int seconds) {
            this.seconds = seconds;
        }
    }

    public static class Retry {
        private int maxAttempts = 2;
        private long backoffMs = 200;

        public int getMaxAttempts() {
            return maxAttempts;
        }

        public void setMaxAttempts(int maxAttempts) {
            this.maxAttempts = maxAttempts;
        }

        public long getBackoffMs() {
            return backoffMs;
        }

        public void setBackoffMs(long backoffMs) {
            this.backoffMs = backoffMs;
        }
    }

    public static class Cache {
        private boolean enabled = true;
        private long ttlSeconds = 300;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public long getTtlSeconds() {
            return ttlSeconds;
        }

        public void setTtlSeconds(long ttlSeconds) {
            this.ttlSeconds = ttlSeconds;
        }
    }

    public static class Scheduler {
        private boolean enabled = true;
        private int workerCount = 2;
        private int queueCapacity = 64;

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public int getWorkerCount() {
            return workerCount;
        }

        public void setWorkerCount(int workerCount) {
            this.workerCount = workerCount;
        }

        public int getQueueCapacity() {
            return queueCapacity;
        }

        public void setQueueCapacity(int queueCapacity) {
            this.queueCapacity = queueCapacity;
        }
    }

    public static class ProviderEndpoint {
        private boolean enabled = false;
        private String baseUrl = "http://127.0.0.1:11434";

        public boolean isEnabled() {
            return enabled;
        }

        public void setEnabled(boolean enabled) {
            this.enabled = enabled;
        }

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }
    }
}
