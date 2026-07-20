package ai.nova.platform.policy.entity;

public enum PolicyEventType {
    CREATED,
    ENABLED,
    DISABLED,
    ARCHIVED,
    EVALUATION_STARTED,
    EVALUATION_COMPLETED,
    IDEMPOTENT_RETURN,
    FAILED
}
