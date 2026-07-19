package ai.nova.platform.rollback.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.rollback.entity.RollbackOperationEntity;

public interface RollbackOperationRepository extends JpaRepository<RollbackOperationEntity, UUID> {

    Optional<RollbackOperationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<RollbackOperationEntity> findByOrganizationIdAndRollbackPlanHash(UUID organizationId, String rollbackPlanHash);

    List<RollbackOperationEntity> findByOrganizationIdOrderByCreatedAtDesc(UUID organizationId);

    List<RollbackOperationEntity> findByOrganizationIdAndProjectIdOrderByCreatedAtDesc(
            UUID organizationId, UUID projectId);
}
