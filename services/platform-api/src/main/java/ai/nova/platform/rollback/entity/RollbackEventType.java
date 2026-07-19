package ai.nova.platform.rollback.entity;

public enum RollbackEventType {
    CREATED,
    VALIDATING,
    VALIDATION_PASSED,
    VALIDATION_FAILED,
    READY,
    CANCELLED,
    FAILED,
    IDEMPOTENT_RETURN
}
