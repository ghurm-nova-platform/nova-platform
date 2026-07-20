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
@Table(name = "policy_versions")
public class PolicyVersionEntity {

    @Id
    private UUID id;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "version_number", nullable = false)
    private int versionNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_type", nullable = false, length = 60)
    private PolicyType policyType;

    @Enumerated(EnumType.STRING)
    @Column(name = "evaluation_mode", nullable = false, length = 30)
    private EvaluationMode evaluationMode;

    @Column(name = "priority", nullable = false)
    private int priority;

    @Column(name = "config_json", nullable = false, columnDefinition = "TEXT")
    private String configJson;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PolicyVersionEntity() {
    }

    public PolicyVersionEntity(
            UUID id,
            UUID policyId,
            int versionNumber,
            PolicyType policyType,
            EvaluationMode evaluationMode,
            int priority,
            String configJson,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.policyId = policyId;
        this.versionNumber = versionNumber;
        this.policyType = policyType;
        this.evaluationMode = evaluationMode;
        this.priority = priority;
        this.configJson = configJson;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public int getVersionNumber() {
        return versionNumber;
    }

    public PolicyType getPolicyType() {
        return policyType;
    }

    public EvaluationMode getEvaluationMode() {
        return evaluationMode;
    }

    public int getPriority() {
        return priority;
    }

    public String getConfigJson() {
        return configJson;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
