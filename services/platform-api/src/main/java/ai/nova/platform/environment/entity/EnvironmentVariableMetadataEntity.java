package ai.nova.platform.environment.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "environment_variables_metadata")
public class EnvironmentVariableMetadataEntity {

    @Id
    private UUID id;

    @Column(name = "environment_id", nullable = false)
    private UUID environmentId;

    @Column(name = "variable_name", nullable = false, length = 200)
    private String variableName;

    @Column(name = "description", length = 2000)
    private String description;

    @Column(name = "required", nullable = false)
    private boolean required;

    @Column(name = "masked", nullable = false)
    private boolean masked;

    @Column(name = "scope", nullable = false, length = 60)
    private String scope;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected EnvironmentVariableMetadataEntity() {
    }

    public EnvironmentVariableMetadataEntity(
            UUID id,
            UUID environmentId,
            String variableName,
            String description,
            boolean required,
            boolean masked,
            String scope,
            Instant createdAt,
            Instant updatedAt) {
        this.id = id;
        this.environmentId = environmentId;
        this.variableName = variableName;
        this.description = description;
        this.required = required;
        this.masked = masked;
        this.scope = scope;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getEnvironmentId() {
        return environmentId;
    }

    public String getVariableName() {
        return variableName;
    }

    public String getDescription() {
        return description;
    }

    public boolean isRequired() {
        return required;
    }

    public boolean isMasked() {
        return masked;
    }

    public String getScope() {
        return scope;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
