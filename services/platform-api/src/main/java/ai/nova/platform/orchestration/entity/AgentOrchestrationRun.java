package ai.nova.platform.orchestration.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "agent_orchestration_runs")
public class AgentOrchestrationRun {

    @Id
    private UUID id;

    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;

    @Column(name = "project_id", nullable = false)
    private UUID projectId;

    @Column(name = "initiated_by_agent_id")
    private UUID initiatedByAgentId;

    @Column(name = "root_execution_id")
    private UUID rootExecutionId;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false, length = 4000)
    private String objective;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RunStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "execution_mode", nullable = false, length = 30)
    private ExecutionMode executionMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "failure_policy", nullable = false, length = 30)
    private FailurePolicy failurePolicy;

    @Column(name = "max_parallel_tasks", nullable = false)
    private int maxParallelTasks;

    @Column(name = "maximum_duration_ms", nullable = false)
    private long maximumDurationMs;

    /**
     * Legacy mirror of the event counter. Updated only via native SQL / migration backfill.
     * JPA must not overwrite it on business saves (see OrchestrationEventService).
     */
    @Column(name = "event_sequence", nullable = false, insertable = true, updatable = false)
    private long eventSequence;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "cancelled_at")
    private Instant cancelledAt;

    @Column(name = "deadline_at")
    private Instant deadlineAt;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "failure_code", length = 100)
    private String failureCode;

    @Column(name = "failure_message", length = 2000)
    private String failureMessage;

    @Column(name = "input_json", columnDefinition = "TEXT")
    private String inputJson;

    @Column(name = "output_json", columnDefinition = "TEXT")
    private String outputJson;

    @Column(name = "metadata_json", columnDefinition = "TEXT")
    private String metadataJson;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "updated_by", nullable = false)
    private UUID updatedBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    @Column(nullable = false)
    private Long version;

    protected AgentOrchestrationRun() {
    }

    public AgentOrchestrationRun(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String name,
            String objective,
            RunStatus status,
            ExecutionMode executionMode,
            FailurePolicy failurePolicy,
            int maxParallelTasks,
            long maximumDurationMs,
            UUID createdBy,
            Instant createdAt) {
        this.id = id;
        this.organizationId = organizationId;
        this.projectId = projectId;
        this.name = name;
        this.objective = objective;
        this.status = status;
        this.executionMode = executionMode;
        this.failurePolicy = failurePolicy;
        this.maxParallelTasks = maxParallelTasks;
        this.maximumDurationMs = maximumDurationMs;
        this.eventSequence = 0L;
        this.createdBy = createdBy;
        this.updatedBy = createdBy;
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

    public UUID getInitiatedByAgentId() {
        return initiatedByAgentId;
    }

    public void setInitiatedByAgentId(UUID initiatedByAgentId) {
        this.initiatedByAgentId = initiatedByAgentId;
    }

    public UUID getRootExecutionId() {
        return rootExecutionId;
    }

    public void setRootExecutionId(UUID rootExecutionId) {
        this.rootExecutionId = rootExecutionId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getObjective() {
        return objective;
    }

    public void setObjective(String objective) {
        this.objective = objective;
    }

    public RunStatus getStatus() {
        return status;
    }

    public void setStatus(RunStatus status) {
        this.status = status;
    }

    public ExecutionMode getExecutionMode() {
        return executionMode;
    }

    public void setExecutionMode(ExecutionMode executionMode) {
        this.executionMode = executionMode;
    }

    public FailurePolicy getFailurePolicy() {
        return failurePolicy;
    }

    public void setFailurePolicy(FailurePolicy failurePolicy) {
        this.failurePolicy = failurePolicy;
    }

    public int getMaxParallelTasks() {
        return maxParallelTasks;
    }

    public void setMaxParallelTasks(int maxParallelTasks) {
        this.maxParallelTasks = maxParallelTasks;
    }

    public long getMaximumDurationMs() {
        return maximumDurationMs;
    }

    public void setMaximumDurationMs(long maximumDurationMs) {
        this.maximumDurationMs = maximumDurationMs;
    }

    public long getEventSequence() {
        return eventSequence;
    }

    public void setEventSequence(long eventSequence) {
        this.eventSequence = eventSequence;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(Instant completedAt) {
        this.completedAt = completedAt;
    }

    public Instant getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Instant cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public Instant getDeadlineAt() {
        return deadlineAt;
    }

    public void setDeadlineAt(Instant deadlineAt) {
        this.deadlineAt = deadlineAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public String getFailureCode() {
        return failureCode;
    }

    public void setFailureCode(String failureCode) {
        this.failureCode = failureCode;
    }

    public String getFailureMessage() {
        return failureMessage;
    }

    public void setFailureMessage(String failureMessage) {
        this.failureMessage = failureMessage;
    }

    public String getInputJson() {
        return inputJson;
    }

    public void setInputJson(String inputJson) {
        this.inputJson = inputJson;
    }

    public String getOutputJson() {
        return outputJson;
    }

    public void setOutputJson(String outputJson) {
        this.outputJson = outputJson;
    }

    public String getMetadataJson() {
        return metadataJson;
    }

    public void setMetadataJson(String metadataJson) {
        this.metadataJson = metadataJson;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public UUID getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(UUID updatedBy) {
        this.updatedBy = updatedBy;
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

    public Long getVersion() {
        return version;
    }
}
