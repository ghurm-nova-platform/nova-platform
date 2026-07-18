package ai.nova.platform.orchestration.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;

@Entity
@Table(name = "agent_task_dependencies")
@IdClass(AgentTaskDependencyId.class)
public class AgentTaskDependency {

    @Column(name = "run_id", nullable = false)
    private UUID runId;

    @Id
    @Column(name = "predecessor_task_id", nullable = false)
    private UUID predecessorTaskId;

    @Id
    @Column(name = "successor_task_id", nullable = false)
    private UUID successorTaskId;

    @Enumerated(EnumType.STRING)
    @Column(name = "dependency_type", nullable = false, length = 30)
    private DependencyType dependencyType;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected AgentTaskDependency() {
    }

    public AgentTaskDependency(
            UUID runId,
            UUID predecessorTaskId,
            UUID successorTaskId,
            DependencyType dependencyType,
            Instant createdAt) {
        this.runId = runId;
        this.predecessorTaskId = predecessorTaskId;
        this.successorTaskId = successorTaskId;
        this.dependencyType = dependencyType;
        this.createdAt = createdAt;
    }

    public UUID getRunId() { return runId; }
    public UUID getPredecessorTaskId() { return predecessorTaskId; }
    public UUID getSuccessorTaskId() { return successorTaskId; }
    public DependencyType getDependencyType() { return dependencyType; }
    public Instant getCreatedAt() { return createdAt; }
}
