package ai.nova.platform.deploymentexecution.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionValidationEntity;

public interface DeploymentExecutionValidationRepository
        extends JpaRepository<DeploymentExecutionValidationEntity, UUID> {

    List<DeploymentExecutionValidationEntity> findByExecutionIdOrderByCreatedAtAsc(UUID executionId);
}
