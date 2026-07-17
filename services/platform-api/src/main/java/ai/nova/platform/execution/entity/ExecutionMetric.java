package ai.nova.platform.execution.entity;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "execution_metrics")
public class ExecutionMetric {

    @Id
    private UUID id;

    @Column(name = "execution_id", nullable = false)
    private UUID executionId;

    @Column(name = "metric_name", nullable = false, length = 100)
    private String metricName;

    @Column(name = "metric_value", nullable = false, length = 500)
    private String metricValue;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected ExecutionMetric() {
    }

    public ExecutionMetric(UUID id, UUID executionId, String metricName, String metricValue, Instant createdAt) {
        this.id = id;
        this.executionId = executionId;
        this.metricName = metricName;
        this.metricValue = metricValue;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public UUID getExecutionId() {
        return executionId;
    }

    public String getMetricName() {
        return metricName;
    }

    public String getMetricValue() {
        return metricValue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
