package ai.nova.platform.ci.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "ci_jobs")
public class CiJobEntity {

    @Id
    private UUID id;

    @Column(name = "ci_workflow_run_id", nullable = false)
    private UUID ciWorkflowRunId;

    @Column(name = "external_job_id", length = 100)
    private String externalJobId;

    @Column(name = "job_name", nullable = false, length = 500)
    private String jobName;

    @Column(nullable = false, length = 40)
    private String status;

    @Column(length = 40)
    private String conclusion;

    @Column(name = "duration_ms")
    private Long durationMs;

    @Column(name = "failure_reason", length = 2000)
    private String failureReason;

    @Column(name = "started_at")
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected CiJobEntity() {
    }

    public CiJobEntity(
            UUID id,
            UUID ciWorkflowRunId,
            String externalJobId,
            String jobName,
            String status,
            String conclusion,
            Long durationMs,
            String failureReason,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
        this.id = id;
        this.ciWorkflowRunId = ciWorkflowRunId;
        this.externalJobId = externalJobId;
        this.jobName = jobName;
        this.status = status;
        this.conclusion = conclusion;
        this.durationMs = durationMs;
        this.failureReason = failureReason;
        this.startedAt = startedAt;
        this.completedAt = completedAt;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getCiWorkflowRunId() {
        return ciWorkflowRunId;
    }

    public String getExternalJobId() {
        return externalJobId;
    }

    public String getJobName() {
        return jobName;
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
