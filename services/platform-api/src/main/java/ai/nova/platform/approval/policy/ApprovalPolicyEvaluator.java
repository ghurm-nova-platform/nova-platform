package ai.nova.platform.approval.policy;

import java.util.List;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.service.ApprovalEvidenceBundle;

public interface ApprovalPolicyEvaluator {

    List<RuleEvaluationResult> evaluate(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy, ApprovalGateProperties properties);
}
