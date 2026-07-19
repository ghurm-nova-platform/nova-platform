package ai.nova.platform.approval.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.entity.ApprovalEvidenceType;
import ai.nova.platform.approval.entity.ApprovalHumanActionType;
import ai.nova.platform.approval.entity.ApprovalPolicyStatus;
import ai.nova.platform.approval.entity.ApprovalRequirementResult;
import ai.nova.platform.approval.entity.ApprovalSeverity;

public final class ApprovalDtos {

    private ApprovalDtos() {
    }

    public record ApprovalRunRequest(@NotNull UUID taskId) {
    }

    public record HumanActionRequest(String comment, String idempotencyKey) {
    }

    public record ApprovalRequirement(
            UUID id,
            String ruleCode,
            String description,
            String expectedValue,
            String actualValue,
            ApprovalRequirementResult result,
            boolean blocking,
            ApprovalSeverity severity,
            String failureReason,
            Instant evaluatedAt) {
    }

    public record ApprovalEvidenceRef(
            UUID id,
            ApprovalEvidenceType evidenceType,
            UUID sourceOperationId,
            UUID sourceResultId,
            String sourceVersion,
            String sourceHash,
            String observedStatus,
            String observedValue,
            Instant createdAt) {
    }

    public record ApprovalTimelineEvent(
            String eventType, String detail, UUID actorUserId, Instant createdAt) {
    }

    public record ApprovalHumanActionView(
            UUID id,
            UUID actorUserId,
            ApprovalHumanActionType action,
            String commentText,
            String evidenceFingerprint,
            Instant createdAt) {
    }

    public record ApprovalDecision(
            UUID id,
            UUID taskId,
            UUID projectId,
            ApprovalDecisionValue decision,
            boolean eligibleForMerge,
            UUID policyId,
            String policyName,
            int policyVersion,
            String evidenceFingerprint,
            String decisionFingerprint,
            UUID patchResultId,
            String patchHash,
            UUID gitOperationId,
            String commitHash,
            UUID pullRequestOperationId,
            Long pullRequestNumber,
            String pullRequestUrl,
            UUID ciObservationOperationId,
            String ciOverallStatus,
            UUID repairOperationId,
            int requiredHumanApprovals,
            int receivedHumanApprovals,
            int rejectionCount,
            String reasonSummary,
            Instant validUntil,
            Map<String, Long> requirementSummaryCounts,
            List<ApprovalTimelineEvent> timelineEvents,
            List<ApprovalHumanActionView> humanActions,
            List<ApprovalRequirement> requirements,
            List<ApprovalEvidenceRef> evidence,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record ApprovalPolicyView(
            UUID id,
            UUID organizationId,
            UUID projectId,
            String name,
            String description,
            int version,
            ApprovalPolicyStatus status,
            boolean isDefault,
            int requiredHumanApprovals,
            boolean requireDistinctApprovers,
            boolean prohibitAuthorApproval,
            boolean requireCiSuccess,
            boolean requireReviewApproved,
            Integer minimumReviewScore,
            boolean requireTestingSuccess,
            Integer minimumEstimatedCoverage,
            boolean requireNoCriticalFindings,
            boolean requireNoHighFindings,
            boolean requireRepairSuccessWhenFailed,
            boolean requirePullRequestOpen,
            boolean requireExactCommitMatch,
            Integer decisionValidityMinutes,
            Instant createdAt,
            Instant updatedAt) {
    }

    public record CreateApprovalPolicyRequest(
            String name,
            String description,
            UUID projectId,
            boolean activate,
            Integer requiredHumanApprovals) {
    }

    public record CreateApprovalPolicyVersionRequest(String description, boolean activate) {
    }
}
