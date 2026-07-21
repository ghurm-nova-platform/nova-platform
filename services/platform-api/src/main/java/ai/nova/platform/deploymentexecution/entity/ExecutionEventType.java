package ai.nova.platform.deploymentexecution.entity;

public enum ExecutionEventType {
    CREATED,
    QUEUED,
    STARTING,
    DEPLOYING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED,
    IDEMPOTENT_RETURN,
    VALIDATION_FAILED
}
