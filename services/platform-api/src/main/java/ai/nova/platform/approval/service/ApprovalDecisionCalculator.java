package ai.nova.platform.approval.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.entity.ApprovalRequirementResult;
import ai.nova.platform.approval.policy.RuleEvaluationResult;

@Service
public class ApprovalDecisionCalculator {

    public DecisionCalculation calculate(
            List<RuleEvaluationResult> rules,
            int requiredHumanApprovals,
            int receivedHumanApprovals,
            int rejectionCount) {
        boolean hasBlockingFailure = rules.stream()
                .anyMatch(r -> r.blocking() && r.result() == ApprovalRequirementResult.FAILED);
        if (hasBlockingFailure) {
            String summary = rules.stream()
                    .filter(r -> r.blocking() && r.result() == ApprovalRequirementResult.FAILED)
                    .map(RuleEvaluationResult::ruleCode)
                    .collect(Collectors.joining(", "));
            return new DecisionCalculation(
                    ApprovalDecisionValue.BLOCKED, false, "Automated requirements failed: " + summary);
        }

        if (rejectionCount > 0) {
            return new DecisionCalculation(ApprovalDecisionValue.REJECTED, false, "Human rejection recorded");
        }

        if (requiredHumanApprovals == 0) {
            return new DecisionCalculation(ApprovalDecisionValue.APPROVED, true, "Automated gate passed; no human approvals required");
        }

        if (receivedHumanApprovals < requiredHumanApprovals) {
            return new DecisionCalculation(
                    ApprovalDecisionValue.REQUIRES_HUMAN_APPROVAL,
                    false,
                    "Automated gate passed; awaiting "
                            + (requiredHumanApprovals - receivedHumanApprovals)
                            + " human approval(s)");
        }

        return new DecisionCalculation(ApprovalDecisionValue.APPROVED, true, "All requirements and human approvals satisfied");
    }

    public record DecisionCalculation(
            ApprovalDecisionValue decision, boolean eligibleForMerge, String reasonSummary) {
    }
}
