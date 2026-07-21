package ai.nova.platform.dashboard.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "nova.dashboard")
public class DashboardProperties {

    private boolean enabled = true;
    private Cache cache = new Cache();
    private int refreshRateSeconds = 30;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Cache getCache() {
        return cache;
    }

    public void setCache(Cache cache) {
        this.cache = cache;
    }

    public int getRefreshRateSeconds() {
        return refreshRateSeconds;
    }

    public void setRefreshRateSeconds(int refreshRateSeconds) {
        this.refreshRateSeconds = refreshRateSeconds;
    }

    public static class Cache {
        private boolean enabled = true;
        private int ttlSeconds = 30;

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
