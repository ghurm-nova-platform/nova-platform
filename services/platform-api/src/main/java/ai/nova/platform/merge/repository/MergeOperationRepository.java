package ai.nova.platform.merge.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.merge.entity.MergeOperationEntity;
import ai.nova.platform.merge.entity.MergeStatus;

public interface MergeOperationRepository extends JpaRepository<MergeOperationEntity, UUID> {

    Optional<MergeOperationEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    List<MergeOperationEntity> findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    Optional<MergeOperationEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);

    Optional<MergeOperationEntity> findByOrganizationIdAndTaskIdAndApprovalDecisionId(
            UUID organizationId, UUID taskId, UUID approvalDecisionId);

    Optional<MergeOperationEntity> findFirstByOrganizationIdAndTaskIdAndApprovalDecisionIdAndStatus(
            UUID organizationId, UUID taskId, UUID approvalDecisionId, MergeStatus status);
}
