package ai.nova.platform.orchestration.entity;

public enum AttemptStatus {
    STARTED,
    SUCCEEDED,
    FAILED,
    CANCELLED,
    TIMED_OUT,
    STALE
}
