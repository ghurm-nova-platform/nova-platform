package ai.nova.platform.deploymentexecution.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deployment_execution_results")
public class DeploymentExecutionResultEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "success", nullable = false)
    private boolean success;

    @Column(name = "summary", length = 2000)
    private String summary;

    @Column(name = "provider_response_json", columnDefinition = "TEXT")
    private String providerResponseJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeploymentExecutionResultEntity() {
    }

    public DeploymentExecutionResultEntity(
            UUID id, UUID executionId, boolean success, String summary, String providerResponseJson, Instant createdAt) {
        this.id = id;
        this.executionId = executionId;
        this.success = success;
        this.summary = summary;
        this.providerResponseJson = providerResponseJson;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getSummary() {
        return summary;
    }

    public String getProviderResponseJson() {
        return providerResponseJson;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
