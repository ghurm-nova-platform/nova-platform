package ai.nova.platform.deployment.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deployment.entity.DeploymentEventEntity;

public interface DeploymentEventRepository extends JpaRepository<DeploymentEventEntity, UUID> {

    List<DeploymentEventEntity> findByDeploymentOperationIdOrderByCreatedAtAsc(UUID deploymentOperationId);
}
