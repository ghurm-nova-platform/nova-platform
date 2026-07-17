package ai.nova.platform.tool.entity;

public enum ToolCallStatus {
    REQUESTED,
    APPROVAL_REQUIRED,
    APPROVED,
    RUNNING,
    COMPLETED,
    FAILED,
    REJECTED,
    CANCELLED
}
