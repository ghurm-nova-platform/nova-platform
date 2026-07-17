package ai.nova.platform.prompt.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "prompt_variables")
public class PromptVariable {

    @Id
    private UUID id;

    @Column(name = "prompt_version_id", nullable = false)
    private UUID promptVersionId;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "data_type", nullable = false, length = 30)
    private PromptVariableDataType dataType;

    @Column(name = "required_flag", nullable = false)
    private boolean required;

    @Column(name = "default_value", columnDefinition = "TEXT")
    private String defaultValue;

    @Column(name = "sample_value", columnDefinition = "TEXT")
    private String sampleValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PromptVariable() {
    }

    public PromptVariable(
            UUID id,
            UUID promptVersionId,
            String name,
            String description,
            PromptVariableDataType dataType,
            boolean required,
            String defaultValue,
            String sampleValue,
            Instant createdAt) {
        this.id = id;
        this.promptVersionId = promptVersionId;
        this.name = name;
        this.description = description;
        this.dataType = dataType;
        this.required = required;
        this.defaultValue = defaultValue;
        this.sampleValue = sampleValue;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPromptVersionId() {
        return promptVersionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public PromptVariableDataType getDataType() {
        return dataType;
    }

    public void setDataType(PromptVariableDataType dataType) {
        this.dataType = dataType;
    }

    public boolean isRequired() {
        return required;
    }

    public void setRequired(boolean required) {
        this.required = required;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public void setDefaultValue(String defaultValue) {
        this.defaultValue = defaultValue;
    }

    public String getSampleValue() {
        return sampleValue;
    }

    public void setSampleValue(String sampleValue) {
        this.sampleValue = sampleValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
