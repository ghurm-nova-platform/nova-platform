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
@Table(name = "policy_events")
public class PolicyEventEntity {

    @Id
    private UUID id;

    @Column(name = "policy_id", nullable = false)
    private UUID policyId;

    @Column(name = "policy_evaluation_id")
    private UUID policyEvaluationId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 60)
    private PolicyEventType eventType;

    @Column(name = "detail", length = 2000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected PolicyEventEntity() {
    }

    public PolicyEventEntity(
            UUID id, UUID policyId, UUID policyEvaluationId, PolicyEventType eventType, String detail, Instant createdAt) {
        this.id = id;
        this.policyId = policyId;
        this.policyEvaluationId = policyEvaluationId;
        this.eventType = eventType;
        this.detail = detail;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getPolicyId() {
        return policyId;
    }

    public UUID getPolicyEvaluationId() {
        return policyEvaluationId;
    }

    public PolicyEventType getEventType() {
        return eventType;
    }

    public String getDetail() {
        return detail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
