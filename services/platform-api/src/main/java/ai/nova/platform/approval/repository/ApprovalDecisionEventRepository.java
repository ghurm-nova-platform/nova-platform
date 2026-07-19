package ai.nova.platform.approval.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.approval.entity.ApprovalDecisionEventEntity;

public interface ApprovalDecisionEventRepository extends JpaRepository<ApprovalDecisionEventEntity, UUID> {

    List<ApprovalDecisionEventEntity> findByApprovalDecisionIdOrderByCreatedAtAsc(UUID approvalDecisionId);
}
