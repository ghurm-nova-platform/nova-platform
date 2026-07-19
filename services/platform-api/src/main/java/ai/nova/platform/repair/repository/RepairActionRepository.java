package ai.nova.platform.repair.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.repair.entity.RepairActionEntity;

public interface RepairActionRepository extends JpaRepository<RepairActionEntity, UUID> {

    List<RepairActionEntity> findByRepairOperationIdOrderByCreatedAtAsc(UUID repairOperationId);
}
