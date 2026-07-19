package ai.nova.platform.repair.repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.repair.entity.RepairResultEntity;

public interface RepairResultRepository extends JpaRepository<RepairResultEntity, UUID> {

    Optional<RepairResultEntity> findByRepairOperationId(UUID repairOperationId);
}
