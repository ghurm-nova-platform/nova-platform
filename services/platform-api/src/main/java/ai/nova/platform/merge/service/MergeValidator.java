package ai.nova.platform.merge.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalDecision;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.service.GitValidator;
import ai.nova.platform.merge.config.MergeProperties;
import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.merge.entity.MergeValidationResult;
import ai.nova.platform.merge.provider.MergeProvider;
import ai.nova.platform.merge.provider.MergeProvider.BranchProtectionStatus;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRecord;
import ai.nova.platform.merge.provider.MergeProvider.RemotePullRequestState;
import ai.nova.platform.pullrequest.provider.RepositoryRef;

@Service
public class MergeValidator {

    public record ValidationCheck(
            String checkCode,
            String expectedValue,
            String actualValue,
            MergeValidationResult result,
            String failureReason,
            boolean blocking) {
    }

    public record ValidationOutcome(List<ValidationCheck> checks, String errorCode, String errorMessage) {
        public boolean passed() {
            return errorCode == null;
        }
    }

    public record MergeValidationContext(
            AgentOrchestrationTask task,
            ApprovalDecision approval,
            PatchResult patch,
            GitOperation git,
            PullRequestOperation pullRequest,
            CiObservationOperation ci,
            MergeMethod mergeMethod,
            MergeProperties properties,
            MergeProvider mergeProvider,
            String githubToken,
            Instant evaluatedAt) {
    }

    private final GitValidator gitValidator;

    public MergeValidator(GitValidator gitValidator) {
        this.gitValidator = gitValidator;
    }

    public ValidationOutcome validate(MergeValidationContext context) {
        List<ValidationCheck> checks = new ArrayList<>();
        checks.add(checkApprovalExists(context));
        checks.add(checkApprovalApproved(context));
        checks.add(checkApprovalNotExpired(context));
        checks.add(checkApprovalNotInvalidated(context));
        checks.add(checkApprovalFingerprintPresent(context));
        checks.add(checkPatchHashUnchanged(context));
        checks.add(checkCommitHashUnchanged(context));
        checks.addAll(checkPullRequest(context));
        checks.add(checkCiSuccess(context));
        checks.add(checkOrgScope(context));
        checks.add(checkProjectScope(context));
        checks.add(checkRepositoryIdentity(context));
        checks.add(checkMethodAllowed(context));
        checks.add(checkBranchProtection(context));

        for (ValidationCheck check : checks) {
            if (check.blocking() && check.result() == MergeValidationResult.FAILED) {
                return new ValidationOutcome(checks, mapErrorCode(check.checkCode(), context.approval()), check.failureReason());
            }
            if (check.blocking() && check.result() == MergeValidationResult.ERROR) {
                return new ValidationOutcome(checks, "MERGE_VALIDATION_FAILED", check.failureReason());
            }
        }
        return new ValidationOutcome(checks, null, null);
    }

    private ValidationCheck checkApprovalExists(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        if (approval == null) {
            return failed("APPROVAL_EXISTS", "present", "missing", "No approval decision found");
        }
        return passed("APPROVAL_EXISTS", "present", "present");
    }

    private ValidationCheck checkApprovalApproved(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        if (approval == null) {
            return skipped("APPROVAL_APPROVED", "APPROVED+eligible", "missing");
        }
        String expected = "APPROVED+eligibleForMerge=true";
        String actual = approval.decision() + "+eligibleForMerge=" + approval.eligibleForMerge();
        if (approval.decision() != ApprovalDecisionValue.APPROVED || !approval.eligibleForMerge()) {
            return failed("APPROVAL_APPROVED", expected, actual, "Approval decision is not eligible for merge");
        }
        return passed("APPROVAL_APPROVED", expected, actual);
    }

    private ValidationCheck checkApprovalNotExpired(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        if (approval == null) {
            return skipped("APPROVAL_NOT_EXPIRED", "valid", "missing");
        }
        Instant validUntil = approval.validUntil();
        if (validUntil != null && !context.evaluatedAt().isBefore(validUntil)) {
            return failed(
                    "APPROVAL_NOT_EXPIRED",
                    "validUntil>" + validUntil,
                    "now=" + context.evaluatedAt(),
                    "Approval decision has expired");
        }
        return passed(
                "APPROVAL_NOT_EXPIRED",
                validUntil == null ? "no-expiry" : validUntil.toString(),
                "valid");
    }

    private ValidationCheck checkApprovalNotInvalidated(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        if (approval == null) {
            return skipped("APPROVAL_NOT_INVALIDATED", "active", "missing");
        }
        ApprovalDecisionValue decision = approval.decision();
        if (decision == ApprovalDecisionValue.INVALIDATED
                || decision == ApprovalDecisionValue.SUPERSEDED
                || decision == ApprovalDecisionValue.EXPIRED
                || decision == ApprovalDecisionValue.REJECTED) {
            return failed(
                    "APPROVAL_NOT_INVALIDATED",
                    "active",
                    decision.name(),
                    "Approval decision is " + decision.name());
        }
        return passed("APPROVAL_NOT_INVALIDATED", "active", decision.name());
    }

    private ValidationCheck checkApprovalFingerprintPresent(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        if (approval == null) {
            return skipped("APPROVAL_FINGERPRINT_PRESENT", "present", "missing");
        }
        String evidence = approval.evidenceFingerprint();
        String decision = approval.decisionFingerprint();
        if (evidence == null
                || evidence.isBlank()
                || decision == null
                || decision.isBlank()) {
            return failed(
                    "APPROVAL_FINGERPRINT_PRESENT",
                    "evidence+decision fingerprints",
                    "evidence="
                            + blankToMissing(evidence)
                            + ",decision="
                            + blankToMissing(decision),
                    "Approval fingerprints are missing");
        }
        return passed("APPROVAL_FINGERPRINT_PRESENT", "present", "present");
    }

    private ValidationCheck checkPatchHashUnchanged(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        PatchResult patch = context.patch();
        if (approval == null || patch == null) {
            return skipped("PATCH_HASH_UNCHANGED", "match", "missing-evidence");
        }
        String computed = patch.patch() != null ? gitValidator.sha256(patch.patch()) : null;
        boolean idMatch = approval.patchResultId().equals(patch.id());
        boolean hashMatch = computed != null && computed.equalsIgnoreCase(approval.patchHash());
        if (!idMatch || !hashMatch) {
            return failed(
                    "PATCH_HASH_UNCHANGED",
                    "patchId=" + approval.patchResultId() + ",hash=" + approval.patchHash(),
                    "patchId=" + patch.id() + ",hash=" + blankToMissing(computed),
                    "Patch evidence changed since approval");
        }
        return passed(
                "PATCH_HASH_UNCHANGED",
                approval.patchHash(),
                computed);
    }

    private ValidationCheck checkCommitHashUnchanged(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        GitOperation git = context.git();
        if (approval == null || git == null) {
            return skipped("COMMIT_HASH_UNCHANGED", "match", "missing-evidence");
        }
        boolean idMatch = approval.gitOperationId().equals(git.id());
        boolean hashMatch =
                git.commitHash() != null && git.commitHash().equalsIgnoreCase(approval.commitHash());
        if (!idMatch || !hashMatch) {
            return failed(
                    "COMMIT_HASH_UNCHANGED",
                    "gitId=" + approval.gitOperationId() + ",hash=" + approval.commitHash(),
                    "gitId=" + git.id() + ",hash=" + blankToMissing(git.commitHash()),
                    "Commit evidence changed since approval");
        }
        return passed("COMMIT_HASH_UNCHANGED", approval.commitHash(), git.commitHash());
    }

    private List<ValidationCheck> checkPullRequest(MergeValidationContext context) {
        List<ValidationCheck> checks = new ArrayList<>();
        checks.add(checkPrHeadUnchanged(context));
        checks.add(checkPrStillOpen(context));
        return checks;
    }

    private ValidationCheck checkPrHeadUnchanged(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        PullRequestOperation pullRequest = context.pullRequest();
        if (approval == null || pullRequest == null) {
            return skipped("PR_HEAD_UNCHANGED", "match", "missing-evidence");
        }
        String actualHead = resolvePrHeadSha(pullRequest, context);
        boolean idMatch = approval.pullRequestOperationId().equals(pullRequest.id());
        boolean hashMatch =
                actualHead != null && actualHead.equalsIgnoreCase(approval.commitHash());
        if (!idMatch || !hashMatch) {
            return failed(
                    "PR_HEAD_UNCHANGED",
                    "prId=" + approval.pullRequestOperationId() + ",head=" + approval.commitHash(),
                    "prId=" + pullRequest.id() + ",head=" + blankToMissing(actualHead),
                    "Pull request head changed since approval");
        }
        return passed("PR_HEAD_UNCHANGED", approval.commitHash(), actualHead);
    }

    private ValidationCheck checkPrStillOpen(MergeValidationContext context) {
        PullRequestOperation pullRequest = context.pullRequest();
        if (pullRequest == null) {
            return failed("PR_STILL_OPEN", "open", "missing", "Pull request operation not found");
        }
        String state = resolvePrState(pullRequest, context);
        if (state == null || !"open".equalsIgnoreCase(state)) {
            return failed("PR_STILL_OPEN", "open", blankToMissing(state), "Pull request is not open");
        }
        return passed("PR_STILL_OPEN", "open", state);
    }

    private ValidationCheck checkCiSuccess(MergeValidationContext context) {
        ApprovalDecision approval = context.approval();
        CiObservationOperation ci = context.ci();
        if (approval == null) {
            return skipped("CI_SUCCESS", "SUCCESS", "missing-approval");
        }
        if (approval.ciObservationOperationId() == null) {
            return failed("CI_SUCCESS", "SUCCESS", "missing", "CI observation evidence is missing from approval");
        }
        if (ci == null) {
            return failed("CI_SUCCESS", "SUCCESS", "missing", "Latest CI observation not found");
        }
        boolean idMatch = approval.ciObservationOperationId().equals(ci.id());
        boolean statusMatch = ci.overallStatus() == CiOverallStatus.SUCCESS;
        boolean commitMatch =
                ci.commitHash() != null && ci.commitHash().equalsIgnoreCase(approval.commitHash());
        boolean approvalStatusMatch =
                approval.ciOverallStatus() != null
                        && approval.ciOverallStatus().equalsIgnoreCase(CiOverallStatus.SUCCESS.name());
        if (!idMatch || !statusMatch || !commitMatch || !approvalStatusMatch) {
            return failed(
                    "CI_SUCCESS",
                    "ciId="
                            + approval.ciObservationOperationId()
                            + ",status=SUCCESS,commit="
                            + approval.commitHash(),
                    "ciId="
                            + ci.id()
                            + ",status="
                            + (ci.overallStatus() != null ? ci.overallStatus().name() : "null")
                            + ",commit="
                            + blankToMissing(ci.commitHash()),
                    "CI evidence changed or is not successful");
        }
        return passed("CI_SUCCESS", CiOverallStatus.SUCCESS.name(), ci.overallStatus().name());
    }

    private ValidationCheck checkOrgScope(MergeValidationContext context) {
        AgentOrchestrationTask task = context.task();
        ApprovalDecision approval = context.approval();
        if (task == null || approval == null) {
            return skipped("ORG_SCOPE", "match", "missing");
        }
        if (!approval.taskId().equals(task.getId())) {
            return failed("ORG_SCOPE", task.getId().toString(), approval.taskId().toString(), "Task scope mismatch");
        }
        return passed("ORG_SCOPE", task.getOrganizationId().toString(), task.getOrganizationId().toString());
    }

    private ValidationCheck checkProjectScope(MergeValidationContext context) {
        AgentOrchestrationTask task = context.task();
        ApprovalDecision approval = context.approval();
        if (task == null || approval == null) {
            return skipped("PROJECT_SCOPE", "match", "missing");
        }
        if (!task.getProjectId().equals(approval.projectId())) {
            return failed(
                    "PROJECT_SCOPE",
                    task.getProjectId().toString(),
                    approval.projectId().toString(),
                    "Project scope mismatch");
        }
        return passed("PROJECT_SCOPE", task.getProjectId().toString(), approval.projectId().toString());
    }

    private ValidationCheck checkRepositoryIdentity(MergeValidationContext context) {
        PullRequestOperation pullRequest = context.pullRequest();
        if (pullRequest == null) {
            return failed("REPOSITORY_IDENTITY", "owner+name", "missing", "Pull request operation not found");
        }
        String owner = pullRequest.repositoryOwner();
        String name = pullRequest.repositoryName();
        if (owner == null || owner.isBlank() || name == null || name.isBlank()) {
            return failed(
                    "REPOSITORY_IDENTITY",
                    "owner+name",
                    "owner=" + blankToMissing(owner) + ",name=" + blankToMissing(name),
                    "Repository identity is incomplete");
        }
        return passed("REPOSITORY_IDENTITY", owner + "/" + name, owner + "/" + name);
    }

    private ValidationCheck checkMethodAllowed(MergeValidationContext context) {
        MergeMethod method = context.mergeMethod();
        List<MergeMethod> allowed = context.properties().resolvedAllowedMethods();
        if (method == null) {
            return failed("METHOD_ALLOWED", allowed.toString(), "null", "Merge method is required");
        }
        if (!allowed.contains(method)) {
            return failed(
                    "METHOD_ALLOWED",
                    allowed.toString(),
                    method.name(),
                    "Merge method is not allowed");
        }
        return passed("METHOD_ALLOWED", allowed.toString(), method.name());
    }

    private ValidationCheck checkBranchProtection(MergeValidationContext context) {
        if (!context.properties().isRequireProtectedBranch()) {
            return skipped("BRANCH_PROTECTED", "protected", "check-disabled");
        }
        PullRequestOperation pullRequest = context.pullRequest();
        if (pullRequest == null || context.mergeProvider() == null) {
            return skipped("BRANCH_PROTECTED", "protected", "missing-context");
        }
        String owner = pullRequest.repositoryOwner();
        String name = pullRequest.repositoryName();
        String targetBranch = pullRequest.targetBranch();
        if (owner == null || owner.isBlank() || name == null || name.isBlank() || targetBranch == null || targetBranch.isBlank()) {
            return failed("BRANCH_PROTECTED", "protected", "missing-repo", "Target branch is unknown");
        }
        try {
            BranchProtectionStatus status = context.mergeProvider()
                    .checkBranchProtection(
                            new RepositoryRef("github.com", owner, name), targetBranch, context.githubToken());
            if (status == BranchProtectionStatus.NOT_PROTECTED) {
                return failed(
                        "BRANCH_PROTECTED",
                        "protected",
                        "not-protected",
                        "Target branch is not protected");
            }
            if (status == BranchProtectionStatus.UNKNOWN) {
                return error("BRANCH_PROTECTED", "protected", "unknown", "Could not determine branch protection");
            }
            return passed("BRANCH_PROTECTED", "protected", "protected");
        } catch (RuntimeException ex) {
            return error("BRANCH_PROTECTED", "protected", "error", ex.getMessage());
        }
    }

    private String resolvePrHeadSha(PullRequestOperation pullRequest, MergeValidationContext context) {
        // Prefer persisted PR evidence for approval alignment. Live remote head is used in
        // post-merge verification, not to override stored pre-merge head during validation.
        if (pullRequest.remoteCommitHash() != null && !pullRequest.remoteCommitHash().isBlank()) {
            return pullRequest.remoteCommitHash();
        }
        if (pullRequest.localCommitHash() != null && !pullRequest.localCommitHash().isBlank()) {
            return pullRequest.localCommitHash();
        }
        RemotePullRequestState remote = refreshPullRequest(pullRequest, context);
        if (remote != null && remote.headSha() != null && !remote.headSha().isBlank()) {
            return remote.headSha();
        }
        return null;
    }

    private String resolvePrState(PullRequestOperation pullRequest, MergeValidationContext context) {
        RemotePullRequestState remote = refreshPullRequest(pullRequest, context);
        if (remote != null) {
            if (remote.isMerged()) {
                return "merged";
            }
            if (remote.state() != null) {
                return remote.state();
            }
        }
        PullRequestRecord record = pullRequest.pullRequestRecord();
        return record != null ? record.state() : null;
    }

    private RemotePullRequestState refreshPullRequest(PullRequestOperation pullRequest, MergeValidationContext context) {
        if (context.mergeProvider() == null
                || pullRequest.pullRequestNumber() == null
                || pullRequest.repositoryOwner() == null
                || pullRequest.repositoryName() == null) {
            return null;
        }
        try {
            return context.mergeProvider()
                    .getPullRequest(
                            new RepositoryRef("github.com", pullRequest.repositoryOwner(), pullRequest.repositoryName()),
                            pullRequest.pullRequestNumber(),
                            context.githubToken());
        } catch (RuntimeException ex) {
            return null;
        }
    }

    private static String mapErrorCode(String checkCode, ApprovalDecision approval) {
        return switch (checkCode) {
            case "APPROVAL_EXISTS", "APPROVAL_APPROVED" -> "MERGE_APPROVAL_REQUIRED";
            case "APPROVAL_NOT_EXPIRED" -> "MERGE_APPROVAL_EXPIRED";
            case "APPROVAL_NOT_INVALIDATED" -> mapInvalidatedCode(approval);
            case "PATCH_HASH_UNCHANGED" -> "MERGE_PATCH_MISMATCH";
            case "COMMIT_HASH_UNCHANGED" -> "MERGE_COMMIT_MISMATCH";
            case "PR_HEAD_UNCHANGED" -> "MERGE_PR_CHANGED";
            case "PR_STILL_OPEN" -> "MERGE_PR_NOT_OPEN";
            case "CI_SUCCESS" -> "MERGE_CI_NOT_SUCCESSFUL";
            case "BRANCH_PROTECTED" -> "MERGE_BRANCH_PROTECTED";
            case "ORG_SCOPE", "PROJECT_SCOPE" -> "MERGE_TASK_NOT_FOUND";
            case "METHOD_ALLOWED", "REPOSITORY_IDENTITY", "APPROVAL_FINGERPRINT_PRESENT" -> "MERGE_VALIDATION_FAILED";
            default -> "MERGE_VALIDATION_FAILED";
        };
    }

    private static String mapInvalidatedCode(ApprovalDecision approval) {
        if (approval == null || approval.decision() == null) {
            return "MERGE_APPROVAL_INVALIDATED";
        }
        return switch (approval.decision()) {
            case SUPERSEDED -> "MERGE_APPROVAL_SUPERSEDED";
            case EXPIRED -> "MERGE_APPROVAL_EXPIRED";
            case INVALIDATED -> "MERGE_APPROVAL_INVALIDATED";
            case REJECTED -> "MERGE_APPROVAL_REQUIRED";
            default -> "MERGE_APPROVAL_INVALIDATED";
        };
    }

    private static ValidationCheck passed(String code, String expected, String actual) {
        return new ValidationCheck(code, expected, actual, MergeValidationResult.PASSED, null, true);
    }

    private static ValidationCheck failed(String code, String expected, String actual, String reason) {
        return new ValidationCheck(code, expected, actual, MergeValidationResult.FAILED, reason, true);
    }

    private static ValidationCheck skipped(String code, String expected, String actual) {
        return new ValidationCheck(code, expected, actual, MergeValidationResult.SKIPPED, null, false);
    }

    private static ValidationCheck error(String code, String expected, String actual, String reason) {
        return new ValidationCheck(code, expected, actual, MergeValidationResult.ERROR, reason, true);
    }

    private static String blankToMissing(String value) {
        return value == null || value.isBlank() ? "missing" : value;
    }
}
