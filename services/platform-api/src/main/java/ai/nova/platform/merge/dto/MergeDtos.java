package ai.nova.platform.merge.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.merge.entity.MergeStatus;
import ai.nova.platform.merge.entity.MergeValidationResult;

public final class MergeDtos {

    private MergeDtos() {
    }

    public record MergeRunRequest(@NotNull UUID taskId, MergeMethod mergeMethod) {
    }

    public record TimelineEvent(String eventType, Instant at, String detail) {
    }

    public record MergeValidationCheck(
            UUID id,
            String checkCode,
            String expectedValue,
            String actualValue,
            MergeValidationResult result,
            String failureReason,
            Instant evaluatedAt) {
    }

    public record MergeResultView(
            UUID id,
            UUID mergeOperationId,
            MergeMethod mergeMethod,
            String mergedCommit,
            long pullRequestNumber,
            String pullRequestUrl,
            Instant mergedAt,
            UUID mergedByUserId,
            String provider,
            String providerMessage,
            Instant createdAt) {
    }

    public record MergeOperation(
            UUID id,
            UUID taskId,
            UUID projectId,
            MergeStatus status,
            MergeMethod mergeMethod,
            UUID approvalDecisionId,
            boolean eligibleForMerge,
            UUID pullRequestOperationId,
            UUID gitOperationId,
            UUID patchResultId,
            UUID ciObservationOperationId,
            long pullRequestNumber,
            String pullRequestUrl,
            String repositoryOwner,
            String repositoryName,
            String evidenceFingerprint,
            String decisionFingerprint,
            String expectedPatchHash,
            String expectedCommitHash,
            String expectedPrHeadSha,
            String errorCode,
            String errorMessage,
            List<MergeValidationCheck> validations,
            List<TimelineEvent> timeline,
            MergeResultView result,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt,
            Instant updatedAt) {
    }
}
