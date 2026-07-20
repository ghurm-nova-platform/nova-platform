package ai.nova.platform.policy.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "release_policies")
public class ReleasePolicyEntity {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "policy_name", nullable = false, length = 200)
    private String policyName;

    @Column(name = "description", length = 2000)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 60)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PolicyStatus status;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_mode", nullable = false, length = 30)
    private EvaluationMode evaluationMode;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "policy_fingerprint", nullable = false, length = 64)
    private String policyFingerprint;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected ReleasePolicyEntity() {
    }

    public ReleasePolicyEntity(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String policyName,
            String description,
            PolicyType policyType,
            PolicyStatus status,
            int priority,
            EvaluationMode evaluationMode,
            String configJson,
            String policyFingerprint,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.policyName = policyName;
        this.description = description;
        this.policyType = policyType;
        this.status = status;
        this.priority = priority;
        this.evaluationMode = evaluationMode;
        this.configJson = configJson;
        this.policyFingerprint = policyFingerprint;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
        this.updatedAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getOrganizationId() {
        return organizationId;
    }

    public UUID getProjectId() {
        return projectId;
    }

    public String getPolicyName() {
        return policyName;
    }

    public String getDescription() {
        return description;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public PolicyStatus getStatus() {
        return status;
    }

    public void setStatus(PolicyStatus status) {
        this.status = status;
    }

    public int getPriority() {
        return priority;
    }

    public EvaluationMode getEvaluationMode() {
        return evaluationMode;
    }

    public String getConfigJson() {
        return configJson;
    }

    public String getPolicyFingerprint() {
        return policyFingerprint;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
