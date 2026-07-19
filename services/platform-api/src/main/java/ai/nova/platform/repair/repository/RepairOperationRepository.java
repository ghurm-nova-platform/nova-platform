package ai.nova.platform.repair.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.repair.entity.RepairOperationEntity;
import ai.nova.platform.repair.entity.RepairStatus;

public interface RepairOperationRepository extends JpaRepository<RepairOperationEntity, UUID> {

    Optional<RepairOperationEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    List<RepairOperationEntity> findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    long countByTaskIdAndOrganizationId(UUID taskId, UUID organizationId);

    Optional<RepairOperationEntity> findByOrganizationIdAndTaskIdAndInputFingerprint(
            UUID organizationId, UUID taskId, String inputFingerprint);

    Optional<RepairOperationEntity> findByOrganizationIdAndTaskIdAndInputFingerprintAndStatus(
            UUID organizationId, UUID taskId, String inputFingerprint, RepairStatus status);
}
