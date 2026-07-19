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
@Table(name = "deployment_environments")
public class DeploymentEnvironmentEntity {

    @Id
    private UUID id;

    @Column(name = "code", nullable = false, length = 40)
    private String code;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "environment_type", nullable = false, length = 30)
    private EnvironmentType environmentType;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "active", nullable = false)
    private boolean active;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeploymentEnvironmentEntity() {
    }

    public DeploymentEnvironmentEntity(
            UUID id,
            String code,
            String name,
            EnvironmentType environmentType,
            int sortOrder,
            boolean active,
            Instant createdAt) {
        this.id = id;
        this.code = code;
        this.name = name;
        this.environmentType = environmentType;
        this.sortOrder = sortOrder;
        this.active = active;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public String getCode() {
        return code;
    }

    public String getName() {
        return name;
    }

    public EnvironmentType getEnvironmentType() {
        return environmentType;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
