package ai.nova.platform.rollback.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.rollback.entity.RollbackPlanEntity;

public interface RollbackPlanRepository extends JpaRepository<RollbackPlanEntity, UUID> {

    Optional<RollbackPlanEntity> findByRollbackOperationId(UUID rollbackOperationId);
}
