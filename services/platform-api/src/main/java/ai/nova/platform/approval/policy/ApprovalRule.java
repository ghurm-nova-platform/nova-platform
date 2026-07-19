package ai.nova.platform.approval.policy;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.service.ApprovalEvidenceBundle;

public interface ApprovalRule {

    String code();

    RuleEvaluationResult evaluate(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy, ApprovalGateProperties properties);
}
