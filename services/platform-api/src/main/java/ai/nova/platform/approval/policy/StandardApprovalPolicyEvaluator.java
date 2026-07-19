package ai.nova.platform.approval.policy;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.entity.ApprovalSeverity;
import ai.nova.platform.approval.service.ApprovalEvidenceBundle;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.git.entity.GitStatus;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.repair.entity.RepairStatus;
import ai.nova.platform.review.entity.ReviewSeverity;

@Service
public class StandardApprovalPolicyEvaluator implements ApprovalPolicyEvaluator {

    private static final Set<String> ALLOWED_TARGET_BRANCHES = Set.of("main", "master", "develop");

    @Override
    public List<RuleEvaluationResult> evaluate(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy, ApprovalGateProperties properties) {
        List<RuleEvaluationResult> results = new ArrayList<>();
        results.add(evaluateTaskExists(bundle));
        results.add(evaluateReviewResultExists(bundle));
        results.add(evaluateReviewApproved(bundle, policy));
        results.add(evaluateReviewScoreMinimum(bundle, policy));
        results.add(evaluateNoCriticalReviewFindings(bundle, policy));
        results.add(evaluateNoHighReviewFindings(bundle, policy));
        results.add(evaluateTestingResultExists(bundle));
        results.add(evaluateTestingSucceeded(bundle, policy));
        results.add(evaluateCoverageMinimum(bundle, policy));
        results.add(evaluatePatchResultExists(bundle));
        results.add(evaluatePatchHashPresent(bundle));
        results.add(evaluatePatchIsLatest(bundle));
        results.add(evaluateGitOperationExists(bundle));
        results.add(evaluateGitOperationSucceeded(bundle));
        results.add(evaluateGitCommitMatchesPatch(bundle, policy));
        results.add(evaluatePullRequestOperationExists(bundle));
        results.add(evaluatePullRequestOperationSucceeded(bundle));
        results.add(evaluatePullRequestIsOpen(bundle, policy));
        results.add(evaluatePullRequestSourceBranchMatches(bundle));
        results.add(evaluatePullRequestTargetBranchAllowed(bundle));
        results.add(evaluatePullRequestHeadCommitMatchesGit(bundle));
        results.add(evaluateCiObservationExists(bundle, policy));
        results.add(evaluateCiCommitMatchesPullRequest(bundle));
        results.add(evaluateCiOverallStatusSuccess(bundle, policy));
        results.add(evaluateRepairRequiredAfterFailure(bundle, policy));
        results.add(evaluateRepairAttemptsWithinLimit(bundle, properties));
        results.add(evaluateNoNewerRelevantResult(bundle));
        results.add(evaluateOrganizationScopeMatch(bundle));
        results.add(evaluateProjectScopeMatch(bundle));
        results.add(evaluateEvidenceIntegrityValid(bundle));
        return results;
    }

    private RuleEvaluationResult evaluateTaskExists(ApprovalEvidenceBundle bundle) {
        if (bundle.task() != null) {
            return RuleEvaluationResult.passed(
                    "TASK_EXISTS", "Task exists", "present", bundle.task().getId().toString(), ApprovalSeverity.INFO);
        }
        return RuleEvaluationResult.failed(
                "TASK_EXISTS", "Task exists", "present", "missing", ApprovalSeverity.CRITICAL, "Task not found");
    }

    private RuleEvaluationResult evaluateReviewResultExists(ApprovalEvidenceBundle bundle) {
        if (bundle.review() != null) {
            return RuleEvaluationResult.passed(
                    "REVIEW_RESULT_EXISTS",
                    "Review result exists",
                    "present",
                    bundle.review().id().toString(),
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "REVIEW_RESULT_EXISTS",
                "Review result exists",
                "present",
                "missing",
                ApprovalSeverity.HIGH,
                "No review result found");
    }

    private RuleEvaluationResult evaluateReviewApproved(ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequireReviewApproved()) {
            return RuleEvaluationResult.notApplicable(
                    "REVIEW_APPROVED", "Review approved", "true", "n/a", ApprovalSeverity.HIGH);
        }
        if (bundle.review() == null) {
            return RuleEvaluationResult.failed(
                    "REVIEW_APPROVED", "Review approved", "true", "missing", ApprovalSeverity.HIGH, "Review missing");
        }
        if (bundle.review().approved()) {
            return RuleEvaluationResult.passed(
                    "REVIEW_APPROVED", "Review approved", "true", "true", ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "REVIEW_APPROVED", "Review approved", "true", "false", ApprovalSeverity.HIGH, "Review not approved");
    }

    private RuleEvaluationResult evaluateReviewScoreMinimum(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        Integer minimum = policy.getMinimumReviewScore();
        if (minimum == null) {
            return RuleEvaluationResult.notApplicable(
                    "REVIEW_SCORE_MINIMUM", "Minimum review score", "n/a", "n/a", ApprovalSeverity.MEDIUM);
        }
        if (bundle.review() == null) {
            return RuleEvaluationResult.failed(
                    "REVIEW_SCORE_MINIMUM",
                    "Minimum review score",
                    String.valueOf(minimum),
                    "missing",
                    ApprovalSeverity.HIGH,
                    "Review missing");
        }
        int score = bundle.review().score();
        if (score >= minimum) {
            return RuleEvaluationResult.passed(
                    "REVIEW_SCORE_MINIMUM",
                    "Minimum review score",
                    String.valueOf(minimum),
                    String.valueOf(score),
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "REVIEW_SCORE_MINIMUM",
                "Minimum review score",
                String.valueOf(minimum),
                String.valueOf(score),
                ApprovalSeverity.HIGH,
                "Review score below minimum");
    }

    private RuleEvaluationResult evaluateNoCriticalReviewFindings(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequireNoCriticalFindings()) {
            return RuleEvaluationResult.notApplicable(
                    "NO_CRITICAL_REVIEW_FINDINGS", "No critical findings", "0", "n/a", ApprovalSeverity.CRITICAL);
        }
        long count = countFindings(bundle, ReviewSeverity.CRITICAL);
        if (count == 0) {
            return RuleEvaluationResult.passed(
                    "NO_CRITICAL_REVIEW_FINDINGS", "No critical findings", "0", "0", ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "NO_CRITICAL_REVIEW_FINDINGS",
                "No critical findings",
                "0",
                String.valueOf(count),
                ApprovalSeverity.CRITICAL,
                "Critical review findings present");
    }

    private RuleEvaluationResult evaluateNoHighReviewFindings(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequireNoHighFindings()) {
            return RuleEvaluationResult.notApplicable(
                    "NO_HIGH_REVIEW_FINDINGS", "No high findings", "0", "n/a", ApprovalSeverity.HIGH);
        }
        long count = countFindings(bundle, ReviewSeverity.HIGH);
        if (count == 0) {
            return RuleEvaluationResult.passed(
                    "NO_HIGH_REVIEW_FINDINGS", "No high findings", "0", "0", ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "NO_HIGH_REVIEW_FINDINGS",
                "No high findings",
                "0",
                String.valueOf(count),
                ApprovalSeverity.HIGH,
                "High review findings present");
    }

    private RuleEvaluationResult evaluateTestingResultExists(ApprovalEvidenceBundle bundle) {
        if (bundle.testing() != null) {
            return RuleEvaluationResult.passed(
                    "TESTING_RESULT_EXISTS",
                    "Testing result exists",
                    "present",
                    bundle.testing().id().toString(),
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "TESTING_RESULT_EXISTS",
                "Testing result exists",
                "present",
                "missing",
                ApprovalSeverity.HIGH,
                "No testing result found");
    }

    private RuleEvaluationResult evaluateTestingSucceeded(ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequireTestingSuccess()) {
            return RuleEvaluationResult.notApplicable(
                    "TESTING_SUCCEEDED", "Testing succeeded", "true", "n/a", ApprovalSeverity.HIGH);
        }
        if (bundle.testing() == null) {
            return RuleEvaluationResult.failed(
                    "TESTING_SUCCEEDED",
                    "Testing succeeded",
                    "true",
                    "missing",
                    ApprovalSeverity.HIGH,
                    "Testing missing");
        }
        boolean validated = bundle.testing().validated();
        boolean summaryOk = bundle.testing().summary() == null
                || !bundle.testing().summary().toLowerCase(Locale.ROOT).contains("fail");
        if (validated && summaryOk) {
            return RuleEvaluationResult.passed(
                    "TESTING_SUCCEEDED", "Testing succeeded", "true", "true", ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "TESTING_SUCCEEDED",
                "Testing succeeded",
                "true",
                "validated=" + validated + ",summaryOk=" + summaryOk,
                ApprovalSeverity.HIGH,
                "Testing did not succeed");
    }

    private RuleEvaluationResult evaluateCoverageMinimum(ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        Integer minimum = policy.getMinimumEstimatedCoverage();
        if (minimum == null) {
            return RuleEvaluationResult.notApplicable(
                    "COVERAGE_MINIMUM", "Minimum coverage", "n/a", "n/a", ApprovalSeverity.MEDIUM);
        }
        if (bundle.testing() == null) {
            return RuleEvaluationResult.failed(
                    "COVERAGE_MINIMUM",
                    "Minimum coverage",
                    String.valueOf(minimum),
                    "missing",
                    ApprovalSeverity.MEDIUM,
                    "Testing missing");
        }
        int coverage = bundle.testing().coverageEstimate();
        if (coverage >= minimum) {
            return RuleEvaluationResult.passed(
                    "COVERAGE_MINIMUM",
                    "Minimum coverage",
                    String.valueOf(minimum),
                    String.valueOf(coverage),
                    ApprovalSeverity.MEDIUM);
        }
        return RuleEvaluationResult.failed(
                "COVERAGE_MINIMUM",
                "Minimum coverage",
                String.valueOf(minimum),
                String.valueOf(coverage),
                ApprovalSeverity.MEDIUM,
                "Coverage below minimum");
    }

    private RuleEvaluationResult evaluatePatchResultExists(ApprovalEvidenceBundle bundle) {
        if (bundle.patch() != null) {
            return RuleEvaluationResult.passed(
                    "PATCH_RESULT_EXISTS",
                    "Patch result exists",
                    "present",
                    bundle.patch().id().toString(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "PATCH_RESULT_EXISTS",
                "Patch result exists",
                "present",
                "missing",
                ApprovalSeverity.CRITICAL,
                "No patch result found");
    }

    private RuleEvaluationResult evaluatePatchHashPresent(ApprovalEvidenceBundle bundle) {
        if (bundle.computedPatchHash() != null && !bundle.computedPatchHash().isBlank()) {
            return RuleEvaluationResult.passed(
                    "PATCH_HASH_PRESENT",
                    "Patch hash present",
                    "present",
                    bundle.computedPatchHash(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "PATCH_HASH_PRESENT",
                "Patch hash present",
                "present",
                "missing",
                ApprovalSeverity.CRITICAL,
                "Patch hash could not be computed");
    }

    private RuleEvaluationResult evaluatePatchIsLatest(ApprovalEvidenceBundle bundle) {
        if (bundle.patch() == null) {
            return RuleEvaluationResult.failed(
                    "PATCH_IS_LATEST", "Patch is latest", "latest", "missing", ApprovalSeverity.HIGH, "Patch missing");
        }
        if (bundle.patch().id().equals(bundle.latestPatchId())) {
            return RuleEvaluationResult.passed(
                    "PATCH_IS_LATEST",
                    "Patch is latest",
                    String.valueOf(bundle.latestPatchId()),
                    bundle.patch().id().toString(),
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "PATCH_IS_LATEST",
                "Patch is latest",
                String.valueOf(bundle.latestPatchId()),
                bundle.patch().id().toString(),
                ApprovalSeverity.HIGH,
                "Patch evidence is stale");
    }

    private RuleEvaluationResult evaluateGitOperationExists(ApprovalEvidenceBundle bundle) {
        if (bundle.git() != null) {
            return RuleEvaluationResult.passed(
                    "GIT_OPERATION_EXISTS",
                    "Git operation exists",
                    "present",
                    bundle.git().id().toString(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "GIT_OPERATION_EXISTS",
                "Git operation exists",
                "present",
                "missing",
                ApprovalSeverity.CRITICAL,
                "No git operation found");
    }

    private RuleEvaluationResult evaluateGitOperationSucceeded(ApprovalEvidenceBundle bundle) {
        if (bundle.git() == null) {
            return RuleEvaluationResult.failed(
                    "GIT_OPERATION_SUCCEEDED",
                    "Git operation succeeded",
                    GitStatus.SUCCEEDED.name(),
                    "missing",
                    ApprovalSeverity.CRITICAL,
                    "Git operation missing");
        }
        if (bundle.git().status() == GitStatus.SUCCEEDED) {
            return RuleEvaluationResult.passed(
                    "GIT_OPERATION_SUCCEEDED",
                    "Git operation succeeded",
                    GitStatus.SUCCEEDED.name(),
                    bundle.git().status().name(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "GIT_OPERATION_SUCCEEDED",
                "Git operation succeeded",
                GitStatus.SUCCEEDED.name(),
                bundle.git().status().name(),
                ApprovalSeverity.CRITICAL,
                "Git operation did not succeed");
    }

    private RuleEvaluationResult evaluateGitCommitMatchesPatch(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequireExactCommitMatch()) {
            return RuleEvaluationResult.notApplicable(
                    "GIT_COMMIT_MATCHES_PATCH", "Git patch hash matches patch", "match", "n/a", ApprovalSeverity.HIGH);
        }
        if (bundle.git() == null || bundle.computedPatchHash() == null) {
            return RuleEvaluationResult.failed(
                    "GIT_COMMIT_MATCHES_PATCH",
                    "Git patch hash matches patch",
                    "match",
                    "missing",
                    ApprovalSeverity.HIGH,
                    "Git or patch hash missing");
        }
        if (bundle.computedPatchHash().equals(bundle.git().patchHash())) {
            return RuleEvaluationResult.passed(
                    "GIT_COMMIT_MATCHES_PATCH",
                    "Git patch hash matches patch",
                    bundle.computedPatchHash(),
                    bundle.git().patchHash(),
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "GIT_COMMIT_MATCHES_PATCH",
                "Git patch hash matches patch",
                bundle.computedPatchHash(),
                bundle.git().patchHash(),
                ApprovalSeverity.HIGH,
                "Git patch hash mismatch");
    }

    private RuleEvaluationResult evaluatePullRequestOperationExists(ApprovalEvidenceBundle bundle) {
        if (bundle.pullRequest() != null) {
            return RuleEvaluationResult.passed(
                    "PULL_REQUEST_OPERATION_EXISTS",
                    "Pull request operation exists",
                    "present",
                    bundle.pullRequest().id().toString(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "PULL_REQUEST_OPERATION_EXISTS",
                "Pull request operation exists",
                "present",
                "missing",
                ApprovalSeverity.CRITICAL,
                "No pull request operation found");
    }

    private RuleEvaluationResult evaluatePullRequestOperationSucceeded(ApprovalEvidenceBundle bundle) {
        if (bundle.pullRequest() == null) {
            return RuleEvaluationResult.failed(
                    "PULL_REQUEST_OPERATION_SUCCEEDED",
                    "Pull request operation succeeded",
                    PullRequestStatus.SUCCEEDED.name(),
                    "missing",
                    ApprovalSeverity.CRITICAL,
                    "Pull request missing");
        }
        if (bundle.pullRequest().status() == PullRequestStatus.SUCCEEDED) {
            return RuleEvaluationResult.passed(
                    "PULL_REQUEST_OPERATION_SUCCEEDED",
                    "Pull request operation succeeded",
                    PullRequestStatus.SUCCEEDED.name(),
                    bundle.pullRequest().status().name(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "PULL_REQUEST_OPERATION_SUCCEEDED",
                "Pull request operation succeeded",
                PullRequestStatus.SUCCEEDED.name(),
                bundle.pullRequest().status().name(),
                ApprovalSeverity.CRITICAL,
                "Pull request operation did not succeed");
    }

    private RuleEvaluationResult evaluatePullRequestIsOpen(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequirePullRequestOpen()) {
            return RuleEvaluationResult.notApplicable(
                    "PULL_REQUEST_IS_OPEN", "Pull request is open", "open", "n/a", ApprovalSeverity.HIGH);
        }
        if (bundle.pullRequest() == null || bundle.pullRequest().pullRequestRecord() == null) {
            return RuleEvaluationResult.failed(
                    "PULL_REQUEST_IS_OPEN",
                    "Pull request is open",
                    "open",
                    "missing",
                    ApprovalSeverity.HIGH,
                    "Pull request record missing");
        }
        String state = bundle.pullRequest().pullRequestRecord().state();
        if (state != null && "open".equalsIgnoreCase(state)) {
            return RuleEvaluationResult.passed(
                    "PULL_REQUEST_IS_OPEN", "Pull request is open", "open", state, ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "PULL_REQUEST_IS_OPEN",
                "Pull request is open",
                "open",
                state,
                ApprovalSeverity.HIGH,
                "Pull request is not open");
    }

    private RuleEvaluationResult evaluatePullRequestSourceBranchMatches(ApprovalEvidenceBundle bundle) {
        if (bundle.pullRequest() == null || bundle.git() == null) {
            return RuleEvaluationResult.failed(
                    "PULL_REQUEST_SOURCE_BRANCH_MATCHES",
                    "PR source branch matches git branch",
                    "match",
                    "missing",
                    ApprovalSeverity.HIGH,
                    "Git or pull request missing");
        }
        String prBranch = bundle.pullRequest().sourceBranch();
        String gitBranch = bundle.git().branchName();
        if (prBranch != null && prBranch.equals(gitBranch)) {
            return RuleEvaluationResult.passed(
                    "PULL_REQUEST_SOURCE_BRANCH_MATCHES",
                    "PR source branch matches git branch",
                    gitBranch,
                    prBranch,
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "PULL_REQUEST_SOURCE_BRANCH_MATCHES",
                "PR source branch matches git branch",
                gitBranch,
                prBranch,
                ApprovalSeverity.HIGH,
                "Source branch mismatch");
    }

    private RuleEvaluationResult evaluatePullRequestTargetBranchAllowed(ApprovalEvidenceBundle bundle) {
        if (bundle.pullRequest() == null) {
            return RuleEvaluationResult.failed(
                    "PULL_REQUEST_TARGET_BRANCH_ALLOWED",
                    "PR target branch allowed",
                    ALLOWED_TARGET_BRANCHES.toString(),
                    "missing",
                    ApprovalSeverity.HIGH,
                    "Pull request missing");
        }
        String target = bundle.pullRequest().targetBranch();
        if (target != null && ALLOWED_TARGET_BRANCHES.contains(target.toLowerCase(Locale.ROOT))) {
            return RuleEvaluationResult.passed(
                    "PULL_REQUEST_TARGET_BRANCH_ALLOWED",
                    "PR target branch allowed",
                    ALLOWED_TARGET_BRANCHES.toString(),
                    target,
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "PULL_REQUEST_TARGET_BRANCH_ALLOWED",
                "PR target branch allowed",
                ALLOWED_TARGET_BRANCHES.toString(),
                target,
                ApprovalSeverity.HIGH,
                "Target branch not allowed");
    }

    private RuleEvaluationResult evaluatePullRequestHeadCommitMatchesGit(ApprovalEvidenceBundle bundle) {
        if (bundle.pullRequest() == null || bundle.git() == null) {
            return RuleEvaluationResult.failed(
                    "PULL_REQUEST_HEAD_COMMIT_MATCHES_GIT_OPERATION",
                    "PR head commit matches git commit",
                    "match",
                    "missing",
                    ApprovalSeverity.HIGH,
                    "Git or pull request missing");
        }
        String prCommit = bundle.pullRequest().localCommitHash();
        String gitCommit = bundle.git().commitHash();
        if (prCommit != null && prCommit.equals(gitCommit)) {
            return RuleEvaluationResult.passed(
                    "PULL_REQUEST_HEAD_COMMIT_MATCHES_GIT_OPERATION",
                    "PR head commit matches git commit",
                    gitCommit,
                    prCommit,
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "PULL_REQUEST_HEAD_COMMIT_MATCHES_GIT_OPERATION",
                "PR head commit matches git commit",
                gitCommit,
                prCommit,
                ApprovalSeverity.HIGH,
                "PR head commit mismatch");
    }

    private RuleEvaluationResult evaluateCiObservationExists(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequireCiSuccess()) {
            return RuleEvaluationResult.notApplicable(
                    "CI_OBSERVATION_EXISTS", "CI observation exists", "present", "n/a", ApprovalSeverity.CRITICAL);
        }
        if (bundle.ci() != null) {
            return RuleEvaluationResult.passed(
                    "CI_OBSERVATION_EXISTS",
                    "CI observation exists",
                    "present",
                    bundle.ci().id().toString(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "CI_OBSERVATION_EXISTS",
                "CI observation exists",
                "present",
                "missing",
                ApprovalSeverity.CRITICAL,
                "No CI observation found");
    }

    private RuleEvaluationResult evaluateCiCommitMatchesPullRequest(ApprovalEvidenceBundle bundle) {
        if (bundle.ci() == null || bundle.pullRequest() == null) {
            return RuleEvaluationResult.failed(
                    "CI_COMMIT_MATCHES_PULL_REQUEST",
                    "CI commit matches pull request",
                    "match",
                    "missing",
                    ApprovalSeverity.HIGH,
                    "CI or pull request missing");
        }
        String ciCommit = bundle.ci().commitHash();
        String prCommit = bundle.pullRequest().localCommitHash();
        if (ciCommit != null && ciCommit.equals(prCommit)) {
            return RuleEvaluationResult.passed(
                    "CI_COMMIT_MATCHES_PULL_REQUEST",
                    "CI commit matches pull request",
                    prCommit,
                    ciCommit,
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "CI_COMMIT_MATCHES_PULL_REQUEST",
                "CI commit matches pull request",
                prCommit,
                ciCommit,
                ApprovalSeverity.HIGH,
                "CI commit mismatch");
    }

    private RuleEvaluationResult evaluateCiOverallStatusSuccess(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequireCiSuccess()) {
            return RuleEvaluationResult.notApplicable(
                    "CI_OVERALL_STATUS_SUCCESS",
                    "CI overall status success",
                    CiOverallStatus.SUCCESS.name(),
                    "n/a",
                    ApprovalSeverity.CRITICAL);
        }
        if (bundle.ci() == null) {
            return RuleEvaluationResult.failed(
                    "CI_OVERALL_STATUS_SUCCESS",
                    "CI overall status success",
                    CiOverallStatus.SUCCESS.name(),
                    "missing",
                    ApprovalSeverity.CRITICAL,
                    "CI missing");
        }
        if (bundle.ci().overallStatus() == CiOverallStatus.SUCCESS) {
            return RuleEvaluationResult.passed(
                    "CI_OVERALL_STATUS_SUCCESS",
                    "CI overall status success",
                    CiOverallStatus.SUCCESS.name(),
                    bundle.ci().overallStatus().name(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "CI_OVERALL_STATUS_SUCCESS",
                "CI overall status success",
                CiOverallStatus.SUCCESS.name(),
                bundle.ci().overallStatus().name(),
                ApprovalSeverity.CRITICAL,
                "CI did not succeed");
    }

    private RuleEvaluationResult evaluateRepairRequiredAfterFailure(
            ApprovalEvidenceBundle bundle, ApprovalPolicyEntity policy) {
        if (!policy.isRequireRepairSuccessWhenFailed()) {
            return RuleEvaluationResult.notApplicable(
                    "REPAIR_REQUIRED_AFTER_FAILURE", "Repair after failure", "succeeded", "n/a", ApprovalSeverity.HIGH);
        }
        boolean reviewApproved = bundle.review() != null && bundle.review().approved();
        boolean ciSuccess = bundle.ci() != null && bundle.ci().overallStatus() == CiOverallStatus.SUCCESS;
        if (ciSuccess && reviewApproved) {
            return RuleEvaluationResult.notApplicable(
                    "REPAIR_REQUIRED_AFTER_FAILURE", "Repair after failure", "succeeded", "n/a", ApprovalSeverity.HIGH);
        }
        if (!bundle.ciHistoryHasFailure()) {
            return RuleEvaluationResult.notApplicable(
                    "REPAIR_REQUIRED_AFTER_FAILURE", "Repair after failure", "succeeded", "no-failure", ApprovalSeverity.HIGH);
        }
        if (bundle.repairSucceededAfterFailure()) {
            return RuleEvaluationResult.passed(
                    "REPAIR_REQUIRED_AFTER_FAILURE",
                    "Repair after failure",
                    "succeeded",
                    "true",
                    ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "REPAIR_REQUIRED_AFTER_FAILURE",
                "Repair after failure",
                "succeeded",
                "false",
                ApprovalSeverity.HIGH,
                "Repair required after prior CI failure");
    }

    private RuleEvaluationResult evaluateRepairAttemptsWithinLimit(
            ApprovalEvidenceBundle bundle, ApprovalGateProperties properties) {
        if (bundle.repair() == null) {
            return RuleEvaluationResult.notApplicable(
                    "REPAIR_ATTEMPTS_WITHIN_LIMIT",
                    "Repair attempts within limit",
                    String.valueOf(properties.getMaxRepairAttempts()),
                    "none",
                    ApprovalSeverity.MEDIUM);
        }
        int attempts = bundle.repair().attemptNumber();
        if (attempts <= properties.getMaxRepairAttempts()) {
            return RuleEvaluationResult.passed(
                    "REPAIR_ATTEMPTS_WITHIN_LIMIT",
                    "Repair attempts within limit",
                    String.valueOf(properties.getMaxRepairAttempts()),
                    String.valueOf(attempts),
                    ApprovalSeverity.MEDIUM);
        }
        return RuleEvaluationResult.failed(
                "REPAIR_ATTEMPTS_WITHIN_LIMIT",
                "Repair attempts within limit",
                String.valueOf(properties.getMaxRepairAttempts()),
                String.valueOf(attempts),
                ApprovalSeverity.MEDIUM,
                "Repair attempts exceeded limit");
    }

    private RuleEvaluationResult evaluateNoNewerRelevantResult(ApprovalEvidenceBundle bundle) {
        boolean fresh = idsMatch(bundle.review() != null ? bundle.review().id() : null, bundle.latestReviewId())
                && idsMatch(bundle.testing() != null ? bundle.testing().id() : null, bundle.latestTestingId())
                && idsMatch(bundle.patch() != null ? bundle.patch().id() : null, bundle.latestPatchId())
                && idsMatch(bundle.git() != null ? bundle.git().id() : null, bundle.latestGitId())
                && idsMatch(
                        bundle.pullRequest() != null ? bundle.pullRequest().id() : null, bundle.latestPullRequestId())
                && idsMatch(bundle.ci() != null ? bundle.ci().id() : null, bundle.latestCiId())
                && idsMatch(bundle.repair() != null ? bundle.repair().id() : null, bundle.latestRepairId());
        if (fresh) {
            return RuleEvaluationResult.passed(
                    "NO_NEWER_RELEVANT_RESULT", "Evidence is latest", "latest", "latest", ApprovalSeverity.HIGH);
        }
        return RuleEvaluationResult.failed(
                "NO_NEWER_RELEVANT_RESULT",
                "Evidence is latest",
                "latest",
                "stale",
                ApprovalSeverity.HIGH,
                "Newer evidence exists");
    }

    private RuleEvaluationResult evaluateOrganizationScopeMatch(ApprovalEvidenceBundle bundle) {
        UUID orgId = bundle.task().getOrganizationId();
        boolean match = bundle.project().getOrganizationId().equals(orgId);
        if (match) {
            return RuleEvaluationResult.passed(
                    "ORGANIZATION_SCOPE_MATCH",
                    "Organization scope match",
                    orgId.toString(),
                    bundle.project().getOrganizationId().toString(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "ORGANIZATION_SCOPE_MATCH",
                "Organization scope match",
                orgId.toString(),
                bundle.project().getOrganizationId().toString(),
                ApprovalSeverity.CRITICAL,
                "Organization scope mismatch");
    }

    private RuleEvaluationResult evaluateProjectScopeMatch(ApprovalEvidenceBundle bundle) {
        UUID projectId = bundle.task().getProjectId();
        boolean match = bundle.project().getId().equals(projectId);
        if (match) {
            return RuleEvaluationResult.passed(
                    "PROJECT_SCOPE_MATCH",
                    "Project scope match",
                    projectId.toString(),
                    bundle.project().getId().toString(),
                    ApprovalSeverity.CRITICAL);
        }
        return RuleEvaluationResult.failed(
                "PROJECT_SCOPE_MATCH",
                "Project scope match",
                projectId.toString(),
                bundle.project().getId().toString(),
                ApprovalSeverity.CRITICAL,
                "Project scope mismatch");
    }

    private RuleEvaluationResult evaluateEvidenceIntegrityValid(ApprovalEvidenceBundle bundle) {
        if (bundle.patch() != null
                && bundle.computedPatchHash() != null
                && bundle.git() != null
                && !bundle.computedPatchHash().equals(bundle.git().patchHash())) {
            return RuleEvaluationResult.failed(
                    "EVIDENCE_INTEGRITY_VALID",
                    "Evidence integrity valid",
                    "consistent",
                    "patch/git-hash-mismatch",
                    ApprovalSeverity.CRITICAL,
                    "Patch and git hash mismatch");
        }
        return RuleEvaluationResult.passed(
                "EVIDENCE_INTEGRITY_VALID", "Evidence integrity valid", "consistent", "consistent", ApprovalSeverity.CRITICAL);
    }

    private static long countFindings(ApprovalEvidenceBundle bundle, ReviewSeverity severity) {
        if (bundle.review() == null || bundle.review().findings() == null) {
            return 0;
        }
        return bundle.review().findings().stream().filter(f -> f.severity() == severity).count();
    }

    private static boolean idsMatch(UUID evidenceId, UUID latestId) {
        if (evidenceId == null && latestId == null) {
            return true;
        }
        return evidenceId != null && evidenceId.equals(latestId);
    }
}
