package ai.nova.platform.deploymentexecution.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deploymentexecution.entity.DeploymentExecutionEntity;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;

public interface DeploymentExecutionRepository extends JpaRepository<DeploymentExecutionEntity, UUID> {

    Optional<DeploymentExecutionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<DeploymentExecutionEntity> findByOrganizationIdAndExecutionFingerprint(
            UUID organizationId, String executionFingerprint);

    List<DeploymentExecutionEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<DeploymentExecutionEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
            UUID organizationId, UUID projectId);

    boolean existsByOrganizationIdAndEnvironmentIdAndStatusIn(
            UUID organizationId, UUID environmentId, Collection<ExecutionStatus> statuses);
}
