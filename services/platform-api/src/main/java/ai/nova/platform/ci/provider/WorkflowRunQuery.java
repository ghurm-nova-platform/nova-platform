package ai.nova.platform.ci.provider;

public record WorkflowRunQuery(String branch, String commitHash, Long pullRequestNumber, int maxRuns) {
}
