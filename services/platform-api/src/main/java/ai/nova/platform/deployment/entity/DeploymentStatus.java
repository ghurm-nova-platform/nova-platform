package ai.nova.platform.deployment.entity;

public enum DeploymentStatus {
    PENDING,
    STARTING,
    RUNNING,
    VERIFYING,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    UNKNOWN
}
