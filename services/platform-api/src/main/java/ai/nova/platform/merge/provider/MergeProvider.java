package ai.nova.platform.merge.provider;

import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.pullrequest.provider.ProviderPullRequest;
import ai.nova.platform.pullrequest.provider.RepositoryRef;

public interface MergeProvider {

    String providerId();

    MergeOutcome merge(MergeRequest request, String token);

    ProviderPullRequest getPullRequest(RepositoryRef repository, long pullRequestNumber, String token);

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

    record MergeOutcome(
            boolean succeeded,
            boolean alreadyMerged,
            String mergedCommitSha,
            String pullRequestUrl,
            String providerMessage) {
    }
}
