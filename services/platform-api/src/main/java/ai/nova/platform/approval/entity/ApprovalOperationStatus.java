package ai.nova.platform.approval.entity;

public enum ApprovalOperationStatus {
    PENDING,
    COLLECTING,
    EVALUATING,
    WAITING_FOR_HUMAN,
    SUCCEEDED,
    FAILED
}
