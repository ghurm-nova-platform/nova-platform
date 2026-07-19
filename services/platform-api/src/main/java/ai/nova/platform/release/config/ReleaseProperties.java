package ai.nova.platform.release.config;

import java.util.Locale;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import ai.nova.platform.release.entity.VersionStrategy;

@Component
@ConfigurationProperties(prefix = "nova.release")
public class ReleaseProperties {

    private boolean enabled = true;
    private boolean allowEditAfterReady = false;
    private boolean allowDelete = false;
    private VersionStrategy defaultVersionStrategy = VersionStrategy.SEMVER;

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean isAllowEditAfterReady() {
        return allowEditAfterReady;
    }

    public void setAllowEditAfterReady(boolean allowEditAfterReady) {
        this.allowEditAfterReady = allowEditAfterReady;
    }

    public boolean isAllowDelete() {
        return allowDelete;
    }

    public void setAllowDelete(boolean allowDelete) {
        this.allowDelete = allowDelete;
    }

    public VersionStrategy getDefaultVersionStrategy() {
        return defaultVersionStrategy;
    }

    public void setDefaultVersionStrategy(VersionStrategy defaultVersionStrategy) {
        this.defaultVersionStrategy = defaultVersionStrategy;
    }

    public void setDefaultVersionStrategy(String defaultVersionStrategy) {
        if (defaultVersionStrategy == null || defaultVersionStrategy.isBlank()) {
            return;
        }
        this.defaultVersionStrategy = VersionStrategy.valueOf(defaultVersionStrategy.trim().toUpperCase(Locale.ROOT));
    }
}
