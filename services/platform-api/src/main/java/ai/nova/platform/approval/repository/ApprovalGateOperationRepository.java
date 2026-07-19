package ai.nova.platform.approval.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.approval.entity.ApprovalGateOperationEntity;

public interface ApprovalGateOperationRepository extends JpaRepository<ApprovalGateOperationEntity, UUID> {

    Optional<ApprovalGateOperationEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    List<ApprovalGateOperationEntity> findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);
}
