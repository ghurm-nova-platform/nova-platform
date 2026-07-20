package ai.nova.platform.environment.entity;

public enum EnvironmentStatus {
    ACTIVE,
    DISABLED,
    MAINTENANCE,
    ARCHIVED;

    public boolean isActiveFlag() {
        return this == ACTIVE || this == MAINTENANCE;
    }
}
