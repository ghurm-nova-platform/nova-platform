package ai.nova.platform.deploymentexecution.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deployment_execution_artifacts")
public class DeploymentExecutionArtifactEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "artifact_type", nullable = false, length = 60)
    private String artifactType;

    @Column(name = "name", nullable = false, length = 200)
    private String name;

    @Column(name = "content_ref", length = 500)
    private String contentRef;

    @Column(name = "checksum", length = 128)
    private String checksum;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected DeploymentExecutionArtifactEntity() {
    }

    public DeploymentExecutionArtifactEntity(
            UUID id,
            UUID executionId,
            String artifactType,
            String name,
            String contentRef,
            String checksum,
            Instant createdAt) {
        this.id = id;
        this.executionId = executionId;
        this.artifactType = artifactType;
        this.name = name;
        this.contentRef = contentRef;
        this.checksum = checksum;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getArtifactType() {
        return artifactType;
    }

    public String getName() {
        return name;
    }

    public String getContentRef() {
        return contentRef;
    }

    public String getChecksum() {
        return checksum;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
