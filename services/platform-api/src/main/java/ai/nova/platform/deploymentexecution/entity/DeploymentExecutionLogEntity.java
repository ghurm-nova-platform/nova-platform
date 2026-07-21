package ai.nova.platform.deploymentexecution.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deployment_execution_logs")
public class DeploymentExecutionLogEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private ExecutionLogLevel level;

    @Column(name = "message", nullable = false, length = 4000)
    private String message;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeploymentExecutionLogEntity() {
    }

    public DeploymentExecutionLogEntity(UUID id, UUID executionId, ExecutionLogLevel level, String message, Instant createdAt) {
        this.id = id;
        this.executionId = executionId;
        this.level = level;
        this.message = message;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public ExecutionLogLevel getLevel() {
        return level;
    }

    public String getMessage() {
        return message;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
