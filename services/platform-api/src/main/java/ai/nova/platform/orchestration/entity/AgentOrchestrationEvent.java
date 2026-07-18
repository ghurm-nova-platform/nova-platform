package ai.nova.platform.orchestration.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_orchestration_events")
public class AgentOrchestrationEvent {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Column(name = "task_id")
    private UUID taskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private OrchestrationEventType eventType;

    @Column(name = "event_sequence", nullable = false)
    private long eventSequence;

    @Column(name = "payload_json", columnDefinition = "TEXT")
    private String payloadJson;

    @Column(name = "created_by")
    private UUID createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AgentOrchestrationEvent() {
    }

    public AgentOrchestrationEvent(
            UUID id,
            UUID organizationId,
            UUID projectId,
            UUID runId,
            UUID taskId,
            OrchestrationEventType eventType,
            long eventSequence,
            String payloadJson,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.runId = runId;
        this.taskId = taskId;
        this.eventType = eventType;
        this.eventSequence = eventSequence;
        this.payloadJson = payloadJson;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public UUID getId() { return id; }
    public UUID getOrganizationId() { return organizationId; }
    public UUID getProjectId() { return projectId; }
    public UUID getRunId() { return runId; }
    public UUID getTaskId() { return taskId; }
    public OrchestrationEventType getEventType() { return eventType; }
    public long getEventSequence() { return eventSequence; }
    public String getPayloadJson() { return payloadJson; }
    public UUID getCreatedBy() { return createdBy; }
    public Instant getCreatedAt() { return createdAt; }
}
