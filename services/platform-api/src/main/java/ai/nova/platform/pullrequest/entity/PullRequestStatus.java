package ai.nova.platform.pullrequest.entity;

public enum PullRequestStatus {
    PENDING,
    VALIDATING,
    PUSHING,
    PUSHED,
    CREATING_PR,
    SUCCEEDED,
    FAILED
}
