package ai.nova.platform.approval.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.approval.entity.ApprovalDecisionEntity;

public interface ApprovalDecisionRepository extends JpaRepository<ApprovalDecisionEntity, UUID> {

    Optional<ApprovalDecisionEntity> findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    List<ApprovalDecisionEntity> findByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
            UUID taskId, UUID organizationId);

    Optional<ApprovalDecisionEntity> findByOrganizationIdAndTaskIdAndEvidenceFingerprintAndDecisionFingerprint(
            UUID organizationId, UUID taskId, String evidenceFingerprint, String decisionFingerprint);

    Optional<ApprovalDecisionEntity> findByIdAndOrganizationId(UUID id, UUID organizationId);
}
