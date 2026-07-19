package ai.nova.platform.merge.provider;

import java.time.Instant;

import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.pullrequest.provider.RepositoryRef;

public interface MergeProvider {

    String providerId();

    /**
     * Attempts a merge. Implementations must not fabricate merge commit SHAs.
     * On ambiguous provider responses, return {@code ambiguous=true} and leave
     * persistence to the agent after remote refetch.
     */
    MergeOutcome merge(MergeRequest request, String token);

    RemotePullRequestState getPullRequest(RepositoryRef repository, long pullRequestNumber, String token);

    BranchProtectionStatus checkBranchProtection(RepositoryRef repository, String branch, String token);

    enum BranchProtectionStatus {
        PROTECTED,
        NOT_PROTECTED,
        UNKNOWN
    }

    record MergeRequest(
            RepositoryRef repository,
            long pullRequestNumber,
            String pullRequestUrl,
            String headSha,
            String targetBranch,
            MergeMethod mergeMethod) {
    }

    /**
     * Provider merge attempt result. {@code mergeCommitSha} is null when unknown —
     * callers must never substitute head SHA.
     */
    record MergeOutcome(
            boolean succeeded,
            boolean alreadyMerged,
            boolean ambiguous,
            String headSha,
            String mergeCommitSha,
            Instant mergedAt,
            String mergedBy,
            String pullRequestUrl,
            String providerMessage) {

        public static MergeOutcome success(
                String headSha,
                String mergeCommitSha,
                Instant mergedAt,
                String mergedBy,
                String pullRequestUrl,
                String providerMessage) {
            return new MergeOutcome(
                    true, false, false, headSha, mergeCommitSha, mergedAt, mergedBy, pullRequestUrl, providerMessage);
        }

        public static MergeOutcome alreadyMerged(
                String headSha,
                String mergeCommitSha,
                Instant mergedAt,
                String mergedBy,
                String pullRequestUrl,
                String providerMessage) {
            return new MergeOutcome(
                    true, true, false, headSha, mergeCommitSha, mergedAt, mergedBy, pullRequestUrl, providerMessage);
        }

        public static MergeOutcome ambiguous(String headSha, String pullRequestUrl, String providerMessage) {
            return new MergeOutcome(false, false, true, headSha, null, null, null, pullRequestUrl, providerMessage);
        }

        public static MergeOutcome failed(String pullRequestUrl, String providerMessage) {
            return new MergeOutcome(false, false, false, null, null, null, null, pullRequestUrl, providerMessage);
        }
    }

    /**
     * Remote PR snapshot used for pre-merge checks and post-merge verification.
     */
    record RemotePullRequestState(
            long number,
            String url,
            String title,
            String sourceBranch,
            String targetBranch,
            String state,
            boolean merged,
            String headSha,
            String mergeCommitSha,
            Instant mergedAt,
            String mergedBy,
            String repositoryOwner,
            String repositoryName) {

        public boolean isMerged() {
            return merged || (state != null && "merged".equalsIgnoreCase(state.trim()));
        }

        public boolean isOpen() {
            return !isMerged() && state != null && "open".equalsIgnoreCase(state.trim());
        }
    }
}
