package ai.nova.platform.deployment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deployment.entity.DeploymentOperationEntity;

public interface DeploymentOperationRepository extends JpaRepository<DeploymentOperationEntity, UUID> {

    Optional<DeploymentOperationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<DeploymentOperationEntity> findByOrganizationIdAndDeploymentHash(UUID organizationId, String deploymentHash);

    List<DeploymentOperationEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
            UUID organizationId, UUID projectId);

    List<DeploymentOperationEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);
}
