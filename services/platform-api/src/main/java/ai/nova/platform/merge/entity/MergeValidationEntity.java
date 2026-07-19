package ai.nova.platform.merge.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "merge_validations")
public class MergeValidationEntity {

    @Id
    private UUID id;

    @Column(name = "merge_operation_id", nullable = false)
    private UUID mergeOperationId;

    @Column(name = "check_code", nullable = false, length = 80)
    private String checkCode;

    @Column(name = "expected_value", length = 2000)
    private String expectedValue;

    @Column(name = "actual_value", length = 2000)
    private String actualValue;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MergeValidationResult result;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "evaluated_at", nullable = false)
    private Instant evaluatedAt;

    protected MergeValidationEntity() {
    }

    public MergeValidationEntity(
            UUID id,
            UUID mergeOperationId,
            String checkCode,
            String expectedValue,
            String actualValue,
            MergeValidationResult result,
            String failureReason,
            Instant evaluatedAt) {
        this.id = id;
        this.mergeOperationId = mergeOperationId;
        this.checkCode = checkCode;
        this.expectedValue = expectedValue;
        this.actualValue = actualValue;
        this.result = result;
        this.failureReason = failureReason;
        this.evaluatedAt = evaluatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getMergeOperationId() {
        return mergeOperationId;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public String getExpectedValue() {
        return expectedValue;
    }

    public String getActualValue() {
        return actualValue;
    }

    public MergeValidationResult getResult() {
        return result;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getEvaluatedAt() {
        return evaluatedAt;
    }
}
