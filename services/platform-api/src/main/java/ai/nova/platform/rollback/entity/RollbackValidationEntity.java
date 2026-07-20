package ai.nova.platform.rollback.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "rollback_validations")
public class RollbackValidationEntity {

    @Id
    private UUID id;

    @Column(name = "rollback_operation_id", nullable = false)
    private UUID rollbackOperationId;

    @Column(name = "check_code", nullable = false, length = 80)
    private String checkCode;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected RollbackValidationEntity() {
    }

    public RollbackValidationEntity(
            UUID id, UUID rollbackOperationId, String checkCode, boolean passed, String message, Instant createdAt) {
        this.id = id;
        this.rollbackOperationId = rollbackOperationId;
        this.checkCode = checkCode;
        this.passed = passed;
        this.message = message;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getRollbackOperationId() {
        return rollbackOperationId;
    }

    public String getCheckCode() {
        return checkCode;
    }

    public boolean isPassed() {
        return passed;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
