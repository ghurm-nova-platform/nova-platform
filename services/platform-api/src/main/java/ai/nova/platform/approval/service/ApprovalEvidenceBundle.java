package ai.nova.platform.approval.service;

import java.util.UUID;

import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.project.Project;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.repair.dto.RepairDtos.RepairOperation;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;

public final class ApprovalEvidenceBundle {

    private final AgentOrchestrationTask task;
    private final Project project;
    private final ReviewResult review;
    private final TestingResult testing;
    private final PatchResult patch;
    private final GitOperation git;
    private final PullRequestOperation pullRequest;
    private final CiObservationOperation ci;
    private final RepairOperation repair;
    private final String computedPatchHash;
    private final UUID latestReviewId;
    private final UUID latestTestingId;
    private final UUID latestPatchId;
    private final UUID latestGitId;
    private final UUID latestPullRequestId;
    private final UUID latestCiId;
    private final UUID latestRepairId;
    private final boolean ciHistoryHasFailure;
    private final boolean repairSucceededAfterFailure;

    public ApprovalEvidenceBundle(
            AgentOrchestrationTask task,
            Project project,
            ReviewResult review,
            TestingResult testing,
            PatchResult patch,
            GitOperation git,
            PullRequestOperation pullRequest,
            CiObservationOperation ci,
            RepairOperation repair,
            String computedPatchHash,
            UUID latestReviewId,
            UUID latestTestingId,
            UUID latestPatchId,
            UUID latestGitId,
            UUID latestPullRequestId,
            UUID latestCiId,
            UUID latestRepairId,
            boolean ciHistoryHasFailure,
            boolean repairSucceededAfterFailure) {
        this.task = task;
        this.project = project;
        this.review = review;
        this.testing = testing;
        this.patch = patch;
        this.git = git;
        this.pullRequest = pullRequest;
        this.ci = ci;
        this.repair = repair;
        this.computedPatchHash = computedPatchHash;
        this.latestReviewId = latestReviewId;
        this.latestTestingId = latestTestingId;
        this.latestPatchId = latestPatchId;
        this.latestGitId = latestGitId;
        this.latestPullRequestId = latestPullRequestId;
        this.latestCiId = latestCiId;
        this.latestRepairId = latestRepairId;
        this.ciHistoryHasFailure = ciHistoryHasFailure;
        this.repairSucceededAfterFailure = repairSucceededAfterFailure;
    }

    public AgentOrchestrationTask task() {
        return task;
    }

    public Project project() {
        return project;
    }

    public ReviewResult review() {
        return review;
    }

    public TestingResult testing() {
        return testing;
    }

    public PatchResult patch() {
        return patch;
    }

    public GitOperation git() {
        return git;
    }

    public PullRequestOperation pullRequest() {
        return pullRequest;
    }

    public CiObservationOperation ci() {
        return ci;
    }

    public RepairOperation repair() {
        return repair;
    }

    public String computedPatchHash() {
        return computedPatchHash;
    }

    public UUID latestReviewId() {
        return latestReviewId;
    }

    public UUID latestTestingId() {
        return latestTestingId;
    }

    public UUID latestPatchId() {
        return latestPatchId;
    }

    public UUID latestGitId() {
        return latestGitId;
    }

    public UUID latestPullRequestId() {
        return latestPullRequestId;
    }

    public UUID latestCiId() {
        return latestCiId;
    }

    public UUID latestRepairId() {
        return latestRepairId;
    }

    public boolean ciHistoryHasFailure() {
        return ciHistoryHasFailure;
    }

    public boolean repairSucceededAfterFailure() {
        return repairSucceededAfterFailure;
    }

    public boolean hasMinimumPersistenceEvidence() {
        return patch != null && git != null && pullRequest != null;
    }
}
