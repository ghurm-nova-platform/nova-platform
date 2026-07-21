package ai.nova.platform.deploymentexecution.entity;

public enum ExecutionStatus {
    READY,
    QUEUED,
    STARTING,
    DEPLOYING,
    VERIFYING,
    COMPLETED,
    FAILED,
    CANCELLED
}
