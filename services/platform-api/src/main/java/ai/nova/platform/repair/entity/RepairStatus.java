package ai.nova.platform.repair.entity;

public enum RepairStatus {
    PENDING,
    COLLECTING,
    ANALYZING,
    GENERATING_PATCH,
    VALIDATING,
    SUCCEEDED,
    FAILED
}
