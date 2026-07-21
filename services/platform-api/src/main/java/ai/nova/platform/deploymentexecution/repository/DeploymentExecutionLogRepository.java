package ai.nova.platform.deploymentexecution.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionLogEntity;

public interface DeploymentExecutionLogRepository extends JpaRepository<DeploymentExecutionLogEntity, UUID> {

    List<DeploymentExecutionLogEntity> findByExecutionIdOrderByCreatedAtAsc(UUID executionId);
}
