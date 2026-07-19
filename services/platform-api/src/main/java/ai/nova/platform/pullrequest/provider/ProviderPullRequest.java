package ai.nova.platform.pullrequest.provider;

public record ProviderPullRequest(
        String externalId,
        long number,
        String url,
        String title,
        String sourceBranch,
        String targetBranch,
        String state,
        String headSha) {
}
