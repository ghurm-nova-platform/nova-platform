package ai.nova.platform.deployment.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deployment_health")
public class DeploymentHealthEntity {

    @Id
    private UUID id;

    @Column(name = "deployment_operation_id", nullable = false)
    private UUID deploymentOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "health", nullable = false, length = 30)
    private DeploymentHealthLevel health;

    @Column(name = "message", length = 2000)
    private String message;

    @Column(name = "observed_at", nullable = false)
    private Instant observedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeploymentHealthEntity() {
    }

    public DeploymentHealthEntity(
            UUID id,
            UUID deploymentOperationId,
            DeploymentHealthLevel health,
            String message,
            Instant observedAt,
            Instant createdAt) {
        this.id = id;
        this.deploymentOperationId = deploymentOperationId;
        this.health = health;
        this.message = message;
        this.observedAt = observedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeploymentOperationId() {
        return deploymentOperationId;
    }

    public DeploymentHealthLevel getHealth() {
        return health;
    }

    public String getMessage() {
        return message;
    }

    public Instant getObservedAt() {
        return observedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
