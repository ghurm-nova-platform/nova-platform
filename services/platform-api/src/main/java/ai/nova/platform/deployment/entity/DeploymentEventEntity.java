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
@Table(name = "deployment_events")
public class DeploymentEventEntity {

    @Id
    private UUID id;

    @Column(name = "deployment_operation_id", nullable = false)
    private UUID deploymentOperationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private DeploymentEventType eventType;

    @Column(name = "detail", length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeploymentEventEntity() {
    }

    public DeploymentEventEntity(
            UUID id, UUID deploymentOperationId, DeploymentEventType eventType, String detail, Instant createdAt) {
        this.id = id;
        this.deploymentOperationId = deploymentOperationId;
        this.eventType = eventType;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getDeploymentOperationId() {
        return deploymentOperationId;
    }

    public DeploymentEventType getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
