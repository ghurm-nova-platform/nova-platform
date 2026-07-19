package ai.nova.platform.pullrequest.provider;

public record CreatePullRequestRequest(
        RepositoryRef repository,
        String title,
        String body,
        String sourceBranch,
        String targetBranch,
        boolean draft) {
}
