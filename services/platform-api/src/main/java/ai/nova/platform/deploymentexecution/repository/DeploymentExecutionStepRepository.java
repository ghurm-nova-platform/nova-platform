package ai.nova.platform.deploymentexecution.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionStepEntity;

public interface DeploymentExecutionStepRepository extends JpaRepository<DeploymentExecutionStepEntity, UUID> {

    List<DeploymentExecutionStepEntity> findByExecutionIdOrderBySortOrderAsc(UUID executionId);
}
