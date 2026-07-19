package ai.nova.platform.ci.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ci_workflow_runs")
public class CiWorkflowRunEntity {

    @Id
    private UUID id;

    @Column(name = "ci_observation_operation_id", nullable = false)
    private UUID ciObservationOperationId;

    @Column(name = "external_workflow_id", length = 100)
    private String externalWorkflowId;

    @Column(name = "workflow_name", nullable = false, length = 500)
    private String workflowName;

    @Column(name = "external_run_id", nullable = false, length = 100)
    private String externalRunId;

    @Column(name = "run_url", length = 2000)
    private String runUrl;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(length = 40)
    private String conclusion;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "trigger_event", length = 100)
    private String triggerEvent;

    @Column(name = "commit_hash", length = 64)
    private String commitHash;

    @Column(length = 255)
    private String branch;

    @Column(name = "pull_request_number")
    private Long pullRequestNumber;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CiWorkflowRunEntity() {
    }

    public CiWorkflowRunEntity(
            UUID id,
            UUID ciObservationOperationId,
            String externalWorkflowId,
            String workflowName,
            String externalRunId,
            String runUrl,
            String status,
            String conclusion,
            Long durationMs,
            String triggerEvent,
            String commitHash,
            String branch,
            Long pullRequestNumber,
            String failureReason,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
        this.id = id;
        this.ciObservationOperationId = ciObservationOperationId;
        this.externalWorkflowId = externalWorkflowId;
        this.workflowName = workflowName;
        this.externalRunId = externalRunId;
        this.runUrl = runUrl;
        this.status = status;
        this.conclusion = conclusion;
        this.durationMs = durationMs;
        this.triggerEvent = triggerEvent;
        this.commitHash = commitHash;
        this.branch = branch;
        this.pullRequestNumber = pullRequestNumber;
        this.failureReason = failureReason;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCiObservationOperationId() {
        return ciObservationOperationId;
    }

    public String getExternalWorkflowId() {
        return externalWorkflowId;
    }

    public String getWorkflowName() {
        return workflowName;
    }

    public String getExternalRunId() {
        return externalRunId;
    }

    public String getRunUrl() {
        return runUrl;
    }

    public String getStatus() {
        return status;
    }

    public String getConclusion() {
        return conclusion;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getTriggerEvent() {
        return triggerEvent;
    }

    public String getCommitHash() {
        return commitHash;
    }

    public String getBranch() {
        return branch;
    }

    public Long getPullRequestNumber() {
        return pullRequestNumber;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public Instant getStartedAt() {
        return startedAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
