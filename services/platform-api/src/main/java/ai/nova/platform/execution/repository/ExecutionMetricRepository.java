package ai.nova.platform.execution.repository;

import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.execution.entity.ExecutionMetric;

public interface ExecutionMetricRepository extends JpaRepository<ExecutionMetric, UUID> {
}
