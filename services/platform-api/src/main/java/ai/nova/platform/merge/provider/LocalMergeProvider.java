package ai.nova.platform.merge.provider;

import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.pullrequest.provider.ProviderPullRequest;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.web.error.ApiException;

/**
 * In-memory merge provider for tests and local development.
 * Never stores or logs credentials.
 */
@Component
@ConditionalOnProperty(name = "nova.merge.provider", havingValue = "LOCAL")
public class LocalMergeProvider implements MergeProvider {

    private static final String PROVIDER_ID = "LOCAL";

    private final ConcurrentMap<String, ProviderPullRequest> pullRequests = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Boolean> protectedBranches = new ConcurrentHashMap<>();

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public MergeOutcome merge(MergeRequest request, String token) {
        requireRepository(request.repository());
        String key = key(request.repository(), request.pullRequestNumber());
        ProviderPullRequest current = pullRequests.get(key);
        if (current != null && "merged".equalsIgnoreCase(current.state())) {
            return new MergeOutcome(
                    true,
                    true,
                    current.headSha(),
                    current.url(),
                    "Pull request already merged");
        }
        String mergedSha = request.headSha() != null ? request.headSha() : "local-merged-sha";
        ProviderPullRequest merged = new ProviderPullRequest(
                current != null ? current.externalId() : String.valueOf(request.pullRequestNumber()),
                request.pullRequestNumber(),
                request.pullRequestUrl() != null ? request.pullRequestUrl() : "https://local/pull/" + request.pullRequestNumber(),
                current != null ? current.title() : "Local PR",
                current != null ? current.sourceBranch() : "feature/local",
                request.targetBranch() != null ? request.targetBranch() : "main",
                "merged",
                mergedSha);
        pullRequests.put(key, merged);
        return new MergeOutcome(true, false, mergedSha, merged.url(), "Merged locally");
    }

    @Override
    public ProviderPullRequest getPullRequest(RepositoryRef repository, long pullRequestNumber, String token) {
        requireRepository(repository);
        String key = key(repository, pullRequestNumber);
        ProviderPullRequest pr = pullRequests.get(key);
        if (pr != null) {
            return pr;
        }
        return new ProviderPullRequest(
                String.valueOf(pullRequestNumber),
                pullRequestNumber,
                "https://local/" + repository.owner() + "/" + repository.name() + "/pull/" + pullRequestNumber,
                "Local PR",
                "feature/local",
                "main",
                "open",
                "local-head-sha");
    }

    @Override
    public BranchProtectionStatus checkBranchProtection(RepositoryRef repository, String branch, String token) {
        requireRepository(repository);
        String key = repository.owner() + "/" + repository.name() + ":" + branch;
        return protectedBranches.getOrDefault(key, false) ? BranchProtectionStatus.PROTECTED : BranchProtectionStatus.NOT_PROTECTED;
    }

    public void registerPullRequest(RepositoryRef repository, ProviderPullRequest pullRequest) {
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
