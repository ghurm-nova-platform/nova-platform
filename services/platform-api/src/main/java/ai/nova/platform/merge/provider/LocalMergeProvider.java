package ai.nova.platform.merge.provider;

import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;

/**
 * In-memory merge provider for tests and local development.
 * Distinguishes headSha from mergeCommitSha. Never stores credentials.
 */
@Component
@ConditionalOnProperty(name = "nova.merge.provider", havingValue = "LOCAL")
public class LocalMergeProvider implements MergeProvider {

    private static final String PROVIDER_ID = "LOCAL";

    private final ConcurrentMap<String, RemotePullRequestState> pullRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> protectedBranches = new ConcurrentHashMap<>();

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public MergeOutcome merge(MergeRequest request, String token) {
        requireRepository(request.repository());
        String key = key(request.repository(), request.pullRequestNumber());
        RemotePullRequestState current = pullRequests.get(key);
        if (current != null && current.isMerged()) {
            return MergeOutcome.alreadyMerged(
                    current.headSha(),
                    current.mergeCommitSha(),
                    current.mergedAt(),
                    current.mergedBy(),
                    current.url(),
                    "Pull request already merged");
        }
        String headSha = request.headSha() != null ? request.headSha() : "local-head-sha";
        // Distinct merge commit — never reuse head SHA as merge commit.
        String mergeCommitSha = "local-merge-" + Integer.toHexString(headSha.hashCode());
        Instant mergedAt = Instant.now();
        RemotePullRequestState merged = new RemotePullRequestState(
                request.pullRequestNumber(),
                request.pullRequestUrl() != null
                        ? request.pullRequestUrl()
                        : "https://local/pull/" + request.pullRequestNumber(),
                current != null ? current.title() : "Local PR",
                current != null ? current.sourceBranch() : "feature/local",
                request.targetBranch() != null ? request.targetBranch() : "main",
                "closed",
                true,
                headSha,
                mergeCommitSha,
                mergedAt,
                "local-bot",
                request.repository().owner(),
                request.repository().name());
        pullRequests.put(key, merged);
        return MergeOutcome.success(headSha, mergeCommitSha, mergedAt, "local-bot", merged.url(), "Merged locally");
    }

    @Override
    public RemotePullRequestState getPullRequest(RepositoryRef repository, long pullRequestNumber, String token) {
        requireRepository(repository);
        String mapKey = key(repository, pullRequestNumber);
        RemotePullRequestState pr = pullRequests.get(mapKey);
        if (pr != null) {
            return pr;
        }
        return new RemotePullRequestState(
                pullRequestNumber,
                "https://local/" + repository.owner() + "/" + repository.name() + "/pull/" + pullRequestNumber,
                "Local PR",
                "feature/local",
                "main",
                "open",
                false,
                "local-head-sha",
                null,
                null,
                null,
                repository.owner(),
                repository.name());
    }

    @Override
    public BranchProtectionStatus checkBranchProtection(RepositoryRef repository, String branch, String token) {
        requireRepository(repository);
        String key = repository.owner() + "/" + repository.name() + ":" + branch;
        return protectedBranches.getOrDefault(key, false)
                ? BranchProtectionStatus.PROTECTED
                : BranchProtectionStatus.NOT_PROTECTED;
    }

    public void registerPullRequest(RepositoryRef repository, RemotePullRequestState pullRequest) {
        pullRequests.put(key(repository, pullRequest.number()), pullRequest);
    }

    public void setBranchProtected(RepositoryRef repository, String branch, boolean protectedBranch) {
        protectedBranches.put(repository.owner() + "/" + repository.name() + ":" + branch, protectedBranch);
    }

    private static String key(RepositoryRef repository, long number) {
        return repository.owner() + "/" + repository.name() + "#" + number;
    }

    private static void requireRepository(RepositoryRef repository) {
        if (repository == null
                || repository.owner() == null
                || repository.owner().isBlank()
                || repository.name() == null
                || repository.name().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "MERGE_INVALID_REQUEST", "Repository owner/name required");
        }
    }
}
