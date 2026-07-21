package ai.nova.platform.deploymentexecution.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionArtifactEntity;

public interface DeploymentExecutionArtifactRepository extends JpaRepository<DeploymentExecutionArtifactEntity, UUID> {

    List<DeploymentExecutionArtifactEntity> findByExecutionIdOrderByCreatedAtAsc(UUID executionId);
}
