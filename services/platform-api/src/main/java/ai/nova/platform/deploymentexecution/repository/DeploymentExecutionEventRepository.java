package ai.nova.platform.deploymentexecution.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEventEntity;

public interface DeploymentExecutionEventRepository extends JpaRepository<DeploymentExecutionEventEntity, UUID> {

    List<DeploymentExecutionEventEntity> findByExecutionIdOrderByCreatedAtAsc(UUID executionId);
}
