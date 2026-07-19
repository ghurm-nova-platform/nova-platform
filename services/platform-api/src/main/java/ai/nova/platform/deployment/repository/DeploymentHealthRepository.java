package ai.nova.platform.deployment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deployment.entity.DeploymentHealthEntity;

public interface DeploymentHealthRepository extends JpaRepository<DeploymentHealthEntity, UUID> {

    List<DeploymentHealthEntity> findByDeploymentOperationIdOrderByObservedAtDesc(UUID deploymentOperationId);
}
