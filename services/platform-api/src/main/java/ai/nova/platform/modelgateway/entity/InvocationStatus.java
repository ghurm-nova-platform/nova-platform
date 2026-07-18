package ai.nova.platform.modelgateway.entity;

public enum InvocationStatus {
    REQUESTED,
    RUNNING,
    COMPLETED,
    FAILED,
    TIMED_OUT,
    CANCELLED,
    RATE_LIMITED
}
