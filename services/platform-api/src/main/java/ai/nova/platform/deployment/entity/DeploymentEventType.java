package ai.nova.platform.deployment.entity;

public enum DeploymentEventType {
    OBSERVED,
    STATUS_CHANGED,
    HEALTH_CHANGED,
    VERIFY_STARTED,
    VERIFY_PASSED,
    VERIFY_FAILED,
    COMPLETED,
    CANCELLED,
    IDEMPOTENT_RETURN
}
