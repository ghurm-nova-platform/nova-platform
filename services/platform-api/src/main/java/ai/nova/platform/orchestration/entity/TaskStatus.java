package ai.nova.platform.orchestration.entity;

public enum TaskStatus {
    DRAFT,
    BLOCKED,
    READY,
    CLAIMED,
    RUNNING,
    RETRY_WAIT,
    WAITING_APPROVAL,
    SUCCEEDED,
    FAILED,
    SKIPPED,
    CANCEL_REQUESTED,
    CANCELLED,
    TIMED_OUT
}
