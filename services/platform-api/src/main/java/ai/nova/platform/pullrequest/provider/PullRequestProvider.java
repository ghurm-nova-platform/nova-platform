package ai.nova.platform.pullrequest.provider;

import java.util.Optional;

public interface PullRequestProvider {

    String providerId();

    void validateRepository(RepositoryRef ref, String token);

    Optional<RemoteBranchInfo> findRemoteBranch(RepositoryRef ref, String branch, String token);

    Optional<ProviderPullRequest> findExistingPullRequest(
            RepositoryRef ref, String sourceBranch, String targetBranch, String token);

    ProviderPullRequest createPullRequest(CreatePullRequestRequest request, String token);

    ProviderPullRequest getPullRequest(RepositoryRef ref, long number, String token);
}
