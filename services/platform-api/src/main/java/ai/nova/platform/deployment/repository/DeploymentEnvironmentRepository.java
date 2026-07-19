package ai.nova.platform.deployment.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.deployment.entity.DeploymentEnvironmentEntity;

public interface DeploymentEnvironmentRepository extends JpaRepository<DeploymentEnvironmentEntity, UUID> {

    Optional<DeploymentEnvironmentEntity> findByCodeIgnoreCase(String code);

    List<DeploymentEnvironmentEntity> findByActiveTrueOrderBySortOrderAsc();
}
