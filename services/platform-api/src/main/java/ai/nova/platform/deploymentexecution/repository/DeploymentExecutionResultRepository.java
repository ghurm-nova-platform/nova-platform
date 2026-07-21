package ai.nova.platform.deploymentexecution.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionResultEntity;

public interface DeploymentExecutionResultRepository extends JpaRepository<DeploymentExecutionResultEntity, UUID> {

    Optional<DeploymentExecutionResultEntity> findByExecutionId(UUID executionId);
}
