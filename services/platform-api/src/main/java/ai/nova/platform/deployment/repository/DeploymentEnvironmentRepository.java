package ai.nova.platform.deployment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;
import ai.nova.platform.deployment.entity.EnvironmentType;

public interface DeploymentEnvironmentRepository extends JpaRepository<DeploymentEnvironmentEntity, UUID> {

    Optional<DeploymentEnvironmentEntity> findByCodeIgnoreCase(String code);

    List<DeploymentEnvironmentEntity> findByActiveTrueOrderBySortOrderAsc();

    Optional<DeploymentEnvironmentEntity> findByOrganizationIdAndProjectIdAndNameIgnoreCase(
            UUID organizationId, UUID projectId, String name);

    List<DeploymentEnvironmentEntity> findByOrganizationIdAndProjectIdOrderBySortOrderAscCreatedAtDesc(
            UUID organizationId, UUID projectId);

    Optional<DeploymentEnvironmentEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    boolean existsByOrganizationIdAndProjectIdAndEnvironmentType(
            UUID organizationId, UUID projectId, EnvironmentType environmentType);
}
