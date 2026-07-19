package ai.nova.platform.approval.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.approval.entity.ApprovalEvidenceEntity;

public interface ApprovalEvidenceRepository extends JpaRepository<ApprovalEvidenceEntity, UUID> {

    List<ApprovalEvidenceEntity> findByApprovalDecisionIdOrderByCreatedAtAsc(UUID approvalDecisionId);
}
