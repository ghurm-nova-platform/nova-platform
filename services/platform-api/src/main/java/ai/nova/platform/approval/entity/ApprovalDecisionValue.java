package ai.nova.platform.approval.entity;

public enum ApprovalDecisionValue {
    PENDING,
    ELIGIBLE,
    BLOCKED,
    REQUIRES_HUMAN_APPROVAL,
    APPROVED,
    REJECTED,
    EXPIRED,
    SUPERSEDED,
    INVALIDATED,
    ERROR
}
