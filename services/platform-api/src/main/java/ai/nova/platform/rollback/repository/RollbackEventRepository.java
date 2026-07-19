package ai.nova.platform.rollback.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.rollback.entity.RollbackEventEntity;

public interface RollbackEventRepository extends JpaRepository<RollbackEventEntity, UUID> {

    List<RollbackEventEntity> findByRollbackOperationIdOrderByCreatedAtAsc(UUID rollbackOperationId);
}
