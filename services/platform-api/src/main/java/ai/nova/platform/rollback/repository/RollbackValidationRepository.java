package ai.nova.platform.rollback.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.rollback.entity.RollbackValidationEntity;

public interface RollbackValidationRepository extends JpaRepository<RollbackValidationEntity, UUID> {

    List<RollbackValidationEntity> findByRollbackOperationIdOrderByCreatedAtAsc(UUID rollbackOperationId);
}
