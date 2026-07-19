package ai.nova.platform.pullrequest.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.entity.RemotePushStatus;

public final class PullRequestDtos {

    private PullRequestDtos() {
    }

    public record PullRequestRunRequest(@NotNull UUID taskId) {
    }

    public record TimelineEvent(String phase, Instant at, String detail) {
    }

    public record PullRequestValidation(boolean valid, String message) {
    }

    public record RemotePushResult(
            UUID id,
            String remoteName,
            String sourceBranch,
            String localCommitHash,
            String remoteCommitHash,
            RemotePushStatus status,
            String errorCode,
            String errorMessage,
            Instant startedAt,
            Instant completedAt) {
    }

    public record PullRequestRecord(
            UUID id,
            String provider,
            String externalId,
            long pullRequestNumber,
            String pullRequestUrl,
            String title,
            String sourceBranch,
            String targetBranch,
            String state,
            Instant createdAt) {
    }

    public record PullRequestOperation(
            UUID id,
            UUID taskId,
            UUID projectId,
            UUID gitOperationId,
            UUID patchResultId,
            PullRequestStatus status,
            String provider,
            String repositoryOwner,
            String repositoryName,
            String remoteName,
            String remoteUrl,
            String sourceBranch,
            String targetBranch,
            String localCommitHash,
            String remoteCommitHash,
            String patchHash,
            Long pullRequestNumber,
            String pullRequestUrl,
            String pullRequestTitle,
            String errorCode,
            PullRequestValidation validation,
            RemotePushResult remotePush,
            PullRequestRecord pullRequestRecord,
            List<TimelineEvent> timeline,
            Instant startedAt,
            Instant completedAt,
            Instant createdAt) {
    }
}
