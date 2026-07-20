package ai.nova.platform.rollback.entity;

public enum RollbackStrategy {
    PREVIOUS_RELEASE,
    PREVIOUS_STABLE,
    SPECIFIC_RELEASE,
    HOTFIX_ONLY,
    CUSTOM
}
