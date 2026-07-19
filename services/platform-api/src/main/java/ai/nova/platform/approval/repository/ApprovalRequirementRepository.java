package ai.nova.platform.approval.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.approval.entity.ApprovalRequirementEntity;

public interface ApprovalRequirementRepository extends JpaRepository<ApprovalRequirementEntity, UUID> {

    List<ApprovalRequirementEntity> findByApprovalDecisionIdOrderByEvaluatedAtAsc(UUID approvalDecisionId);
}
