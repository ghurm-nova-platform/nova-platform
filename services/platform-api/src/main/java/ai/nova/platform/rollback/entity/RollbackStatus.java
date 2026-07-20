package ai.nova.platform.rollback.entity;

public enum RollbackStatus {
    DRAFT,
    VALIDATING,
    READY,
    EXECUTING,
    SUCCEEDED,
    FAILED,
    CANCELLED
}
