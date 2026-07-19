package ai.nova.platform.repair.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.repair.entity.RepairInputEntity;

public interface RepairInputRepository extends JpaRepository<RepairInputEntity, UUID> {

    List<RepairInputEntity> findByRepairOperationIdOrderByPriorityAscCreatedAtAsc(UUID repairOperationId);
}
