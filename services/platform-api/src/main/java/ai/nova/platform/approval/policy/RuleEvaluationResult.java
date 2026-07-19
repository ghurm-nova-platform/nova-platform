package ai.nova.platform.approval.policy;

import ai.nova.platform.approval.entity.ApprovalRequirementResult;
import ai.nova.platform.approval.entity.ApprovalSeverity;

public record RuleEvaluationResult(
        String ruleCode,
        String description,
        String expectedValue,
        String actualValue,
        ApprovalRequirementResult result,
        boolean blocking,
        ApprovalSeverity severity,
        String failureReason) {

    public static RuleEvaluationResult passed(
            String ruleCode, String description, String expectedValue, String actualValue, ApprovalSeverity severity) {
        return new RuleEvaluationResult(
                ruleCode, description, expectedValue, actualValue, ApprovalRequirementResult.PASSED, true, severity, null);
    }

    public static RuleEvaluationResult failed(
            String ruleCode,
            String description,
            String expectedValue,
            String actualValue,
            ApprovalSeverity severity,
            String failureReason) {
        return new RuleEvaluationResult(
                ruleCode,
                description,
                expectedValue,
                actualValue,
                ApprovalRequirementResult.FAILED,
                true,
                severity,
                failureReason);
    }

    public static RuleEvaluationResult notApplicable(
            String ruleCode, String description, String expectedValue, String actualValue, ApprovalSeverity severity) {
        return new RuleEvaluationResult(
                ruleCode,
                description,
                expectedValue,
                actualValue,
                ApprovalRequirementResult.NOT_APPLICABLE,
                false,
                severity,
                null);
    }

    public static RuleEvaluationResult error(
            String ruleCode, String description, String expectedValue, String actualValue, String failureReason) {
        return new RuleEvaluationResult(
                ruleCode,
                description,
                expectedValue,
                actualValue,
                ApprovalRequirementResult.ERROR,
                true,
                ApprovalSeverity.CRITICAL,
                failureReason);
    }
}
