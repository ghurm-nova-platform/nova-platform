package ai.nova.platform.approval.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import ai.nova.platform.approval.entity.ApprovalPolicyRuleEntity;

public interface ApprovalPolicyRuleRepository extends JpaRepository<ApprovalPolicyRuleEntity, UUID> {

    List<ApprovalPolicyRuleEntity> findByApprovalPolicyIdOrderByDisplayOrderAsc(UUID approvalPolicyId);
}
