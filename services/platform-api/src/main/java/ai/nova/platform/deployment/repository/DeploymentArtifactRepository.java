package ai.nova.platform.deployment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deployment.entity.DeploymentArtifactEntity;

public interface DeploymentArtifactRepository extends JpaRepository<DeploymentArtifactEntity, UUID> {

    List<DeploymentArtifactEntity> findByDeploymentOperationIdOrderByCreatedAtAsc(UUID deploymentOperationId);
}
