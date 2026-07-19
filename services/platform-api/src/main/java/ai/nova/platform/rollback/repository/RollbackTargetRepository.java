package ai.nova.platform.rollback.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.rollback.entity.RollbackTargetEntity;

public interface RollbackTargetRepository extends JpaRepository<RollbackTargetEntity, UUID> {

    List<RollbackTargetEntity> findByRollbackOperationIdOrderBySortOrderAsc(UUID rollbackOperationId);
}
