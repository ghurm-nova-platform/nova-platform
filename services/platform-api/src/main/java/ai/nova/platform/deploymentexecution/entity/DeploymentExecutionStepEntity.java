package ai.nova.platform.deploymentexecution.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "deployment_execution_steps")
public class DeploymentExecutionStepEntity {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "step_key", nullable = false, length = 80)
    private String stepKey;

    @Column(name = "stage", nullable = false, length = 80)
    private String stage;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private ExecutionStepStatus status;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    @Column(name = "detail", length = 2000)
    private String detail;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "finished_at")
    private Instant finishedAt;

    protected DeploymentExecutionStepEntity() {
    }

    public DeploymentExecutionStepEntity(
            UUID id,
            UUID executionId,
            String stepKey,
            String stage,
            ExecutionStepStatus status,
            int sortOrder,
            String detail,
            Instant startedAt,
            Instant finishedAt) {
        this.id = id;
        this.executionId = executionId;
        this.stepKey = stepKey;
        this.stage = stage;
        this.status = status;
        this.sortOrder = sortOrder;
        this.detail = detail;
        this.startedAt = startedAt;
        this.finishedAt = finishedAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getStepKey() {
        return stepKey;
    }

    public String getStage() {
        return stage;
    }

    public ExecutionStepStatus getStatus() {
        return status;
    }

    public void setStatus(ExecutionStepStatus status) {
        this.status = status;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getFinishedAt() {
        return finishedAt;
    }

    public void setFinishedAt(Instant finishedAt) {
        this.finishedAt = finishedAt;
    }
}
