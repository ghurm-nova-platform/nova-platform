package ai.nova.platform.pullrequest.provider;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import ai.nova.platform.web.error.ApiException;

/**
 * In-memory pull request provider for tests and local file remotes.
 * Never stores or logs credentials.
 */
@Component
@ConditionalOnProperty(name = "nova.pull-request.provider", havingValue = "LOCAL")
public class InMemoryPullRequestProvider implements PullRequestProvider {

    private static final String PROVIDER_ID = "LOCAL";

    private final AtomicLong pullRequestSequence = new AtomicLong(1);
    private final Map<String, RemoteBranchInfo> branches = new ConcurrentHashMap<>();
    private final Map<String, ProviderPullRequest> pullRequestsByKey = new ConcurrentHashMap<>();
    private final Map<Long, ProviderPullRequest> pullRequestsByNumber = new ConcurrentHashMap<>();

    @Override
    public String providerId() {
        return PROVIDER_ID;
    }

    @Override
    public void validateRepository(RepositoryRef ref, String token) {
        if (ref.owner() == null || ref.owner().isBlank() || ref.name() == null || ref.name().isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "PR_REPOSITORY_MISMATCH", "Repository owner/name required");
        }
    }

    @Override
    public Optional<RemoteBranchInfo> findRemoteBranch(RepositoryRef ref, String branch, String token) {
        return Optional.ofNullable(branches.get(branchKey(ref, branch)));
    }

    @Override
    public Optional<ProviderPullRequest> findExistingPullRequest(
            RepositoryRef ref, String sourceBranch, String targetBranch, String token) {
        return Optional.ofNullable(pullRequestsByKey.get(pullRequestKey(ref, sourceBranch, targetBranch)));
    }

    @Override
    public ProviderPullRequest createPullRequest(CreatePullRequestRequest request, String token) {
        RepositoryRef ref = request.repository();
        String key = pullRequestKey(ref, request.sourceBranch(), request.targetBranch());
        ProviderPullRequest existing = pullRequestsByKey.get(key);
        if (existing != null) {
            return existing;
        }
        RemoteBranchInfo branch = branches.get(branchKey(ref, request.sourceBranch()));
        String headSha = branch == null ? "unknown" : branch.commitHash();
        long number = pullRequestSequence.getAndIncrement();
        String url = "memory://"
                + ref.owner()
                + "/"
                + ref.name()
                + "/pull/"
                + number;
        ProviderPullRequest created = new ProviderPullRequest(
                String.valueOf(number),
                number,
                url,
                request.title(),
                request.sourceBranch(),
                request.targetBranch(),
                request.draft() ? "draft" : "open",
                headSha);
        pullRequestsByKey.put(key, created);
        pullRequestsByNumber.put(number, created);
        return created;
    }

    @Override
    public ProviderPullRequest getPullRequest(RepositoryRef ref, long number, String token) {
        ProviderPullRequest pr = pullRequestsByNumber.get(number);
        if (pr == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "PR_VERIFY_FAILED", "In-memory pull request not found");
        }
        return pr;
    }

    public void recordRemoteBranch(RepositoryRef ref, String branch, String commitHash) {
        branches.put(branchKey(ref, branch), new RemoteBranchInfo(branch, commitHash));
    }

    private static String branchKey(RepositoryRef ref, String branch) {
        return ref.owner() + "/" + ref.name() + "#" + branch;
    }

    private static String pullRequestKey(RepositoryRef ref, String sourceBranch, String targetBranch) {
        return ref.owner() + "/" + ref.name() + ":" + sourceBranch + "->" + targetBranch;
    }
}
