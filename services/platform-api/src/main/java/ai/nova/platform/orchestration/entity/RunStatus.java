package ai.nova.platform.orchestration.entity;

public enum RunStatus {
    DRAFT,
    READY,
    RUNNING,
    WAITING,
    SUCCEEDED,
    PARTIALLY_SUCCEEDED,
    FAILED,
    CANCEL_REQUESTED,
    CANCELLED,
    TIMED_OUT,
    ARCHIVED
}
