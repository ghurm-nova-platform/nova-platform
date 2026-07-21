package ai.nova.platform.deploymentexecution.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deployment_execution_validations")
public class DeploymentExecutionValidationEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "check_code", nullable = false, length = 80)
    private String checkCode;

    @Column(name = "passed", nullable = false)
    private boolean passed;

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeploymentExecutionValidationEntity() {
    }

    public DeploymentExecutionValidationEntity(
            UUID id, UUID executionId, String checkCode, boolean passed, String message, Instant createdAt) {
        this.id = id;
        this.executionId = executionId;
        this.checkCode = checkCode;
        this.passed = passed;
        this.message = message;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
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
