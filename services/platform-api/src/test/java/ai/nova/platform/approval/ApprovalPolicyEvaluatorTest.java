package ai.nova.platform.approval;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.approval.config.ApprovalGateProperties;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.entity.ApprovalPolicyEntity;
import ai.nova.platform.approval.entity.ApprovalPolicyStatus;
import ai.nova.platform.approval.entity.ApprovalRequirementResult;
import ai.nova.platform.approval.policy.ApprovalPolicyEvaluator;
import ai.nova.platform.approval.policy.RuleEvaluationResult;
import ai.nova.platform.approval.service.ApprovalEvidenceBundle;
import ai.nova.platform.approval.support.ApprovalTestFixture;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.entity.CiObservationStatus;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.entity.GitStatus;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.project.ProjectStatus;
import ai.nova.platform.project.ProjectVisibility;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.dto.PatchDtos.PatchStatistics;
import ai.nova.platform.patch.dto.PatchDtos.PatchValidation;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.project.Project;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRecord;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestValidation;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.review.dto.ReviewDtos.ReviewResult;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;

@SpringBootTest
class ApprovalPolicyEvaluatorTest {

    @Autowired
    private ApprovalPolicyEvaluator evaluator;

    @Autowired
    private ApprovalGateProperties properties;

    @Test
    void allRulesPassForCompleteBundle() {
        ApprovalEvidenceBundle bundle = completeBundle("abc", "abc", "main");
        ApprovalPolicyEntity policy = defaultPolicy();
        List<RuleEvaluationResult> results = evaluator.evaluate(bundle, policy, properties);
        assertThat(results).isNotEmpty();
        assertThat(results.stream().filter(r -> r.result() == ApprovalRequirementResult.FAILED)).isEmpty();
    }

    @Test
    void missingReviewFailsBlockingRules() {
        ApprovalEvidenceBundle bundle = completeBundle("abc", "abc", "main");
        ApprovalEvidenceBundle noReview = new ApprovalEvidenceBundle(
                bundle.task(),
                bundle.project(),
                null,
                bundle.testing(),
                bundle.patch(),
                bundle.git(),
                bundle.pullRequest(),
                bundle.ci(),
                null,
                bundle.computedPatchHash(),
                null,
                bundle.latestTestingId(),
                bundle.latestPatchId(),
                bundle.latestGitId(),
                bundle.latestPullRequestId(),
                bundle.latestCiId(),
                null,
                false,
                false);
        List<RuleEvaluationResult> results = evaluator.evaluate(noReview, defaultPolicy(), properties);
        assertThat(results.stream().anyMatch(r -> "REVIEW_RESULT_EXISTS".equals(r.ruleCode())
                && r.result() == ApprovalRequirementResult.FAILED))
                .isTrue();
    }

    private static ApprovalPolicyEntity defaultPolicy() {
        Instant now = Instant.now();
        return new ApprovalPolicyEntity(
                ApprovalTestFixture.DEFAULT_POLICY_ID,
                ApprovalTestFixture.ORG_ID,
                null,
                "Default",
                null,
                1,
                ApprovalPolicyStatus.ACTIVE,
                true,
                1,
                true,
                true,
                true,
                true,
                70,
                true,
                50,
                true,
                false,
                true,
                true,
                true,
                1440,
                ApprovalTestFixture.USER_ID,
                now,
                ApprovalTestFixture.USER_ID,
                now);
    }

    private static ApprovalEvidenceBundle completeBundle(String patchHash, String gitPatchHash, String targetBranch) {
        UUID taskId = UUID.randomUUID();
        UUID org = ApprovalTestFixture.ORG_ID;
        UUID projectId = UUID.fromString(ApprovalTestFixture.PROJECT_ID);
        UUID patchId = UUID.randomUUID();
        UUID gitId = UUID.randomUUID();
        UUID prId = UUID.randomUUID();
        UUID ciId = UUID.randomUUID();
        UUID reviewId = UUID.randomUUID();
        UUID testingId = UUID.randomUUID();
        String commit = "commit1234567890abcdef1234567890abcdef1234567890abcdef1234567890ab";

        AgentOrchestrationTask task = new AgentOrchestrationTask(
                taskId,
                org,
                projectId,
                UUID.randomUUID(),
                "key",
                "Task",
                TaskType.AGENT_TURN,
                TaskStatus.READY,
                "idem",
                1,
                0L,
                1,
                60,
                ApprovalTestFixture.USER_ID,
                Instant.now());
        Project project = new Project(
                projectId,
                org,
                "Demo",
                "demo",
                ProjectStatus.ACTIVE,
                ProjectVisibility.PRIVATE,
                Instant.now(),
                Instant.now(),
                ApprovalTestFixture.USER_ID,
                ApprovalTestFixture.USER_ID);

        PatchResult patch = new PatchResult(
                patchId,
                taskId,
                task.getRunId(),
                projectId,
                "patch",
                PatchStatus.VALID,
                new PatchStatistics(1, 1, 0, 100),
                "patch-content",
                List.of(),
                List.of(),
                new PatchValidation(true, "ok"),
                1L,
                "m",
                "LOCAL",
                1L,
                Instant.now());

        ReviewResult review = new ReviewResult(
                reviewId,
                taskId,
                task.getRunId(),
                projectId,
                "ok",
                85,
                true,
                List.of(),
                List.of(),
                java.util.Map.of(),
                1L,
                "m",
                "LOCAL",
                1L,
                Instant.now(),
                true);

        TestingResult testing = new TestingResult(
                testingId,
                taskId,
                task.getRunId(),
                projectId,
                "ok",
                80,
                List.of(),
                List.of(),
                List.of(),
                java.util.Map.of(),
                java.util.Map.of(),
                1L,
                "m",
                "LOCAL",
                1L,
                Instant.now(),
                true);

        GitOperation git = new GitOperation(
                gitId,
                taskId,
                task.getRunId(),
                projectId,
                patchId,
                GitStatus.SUCCEEDED,
                "feature/login",
                commit,
                gitPatchHash,
                "/repo",
                "main",
                null,
                null,
                null,
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now(),
                Instant.now());

        PullRequestOperation pr = new PullRequestOperation(
                prId,
                taskId,
                projectId,
                gitId,
                patchId,
                PullRequestStatus.SUCCEEDED,
                "LOCAL",
                "owner",
                "repo",
                "origin",
                "http://localhost/repo.git",
                "feature/login",
                targetBranch,
                commit,
                commit,
                gitPatchHash,
                42L,
                "http://localhost/pr/42",
                "title",
                null,
                new PullRequestValidation(true, "ok"),
                null,
                new PullRequestRecord(
                        UUID.randomUUID(), "LOCAL", "42", 42L, "url", "title", "feature/login", targetBranch, "open", Instant.now()),
                List.of(),
                Instant.now(),
                Instant.now(),
                Instant.now());

        CiObservationOperation ci = new CiObservationOperation(
                ciId,
                taskId,
                projectId,
                prId,
                CiObservationStatus.SUCCEEDED,
                "LOCAL",
                "owner",
                "repo",
                "feature/login",
                targetBranch,
                commit,
                42L,
                CiOverallStatus.SUCCESS,
                null,
                null,
                List.of(),
                null,
                null,
                List.of(),
                Instant.now(),
                Instant.now(),
                Instant.now());

        return new ApprovalEvidenceBundle(
                task,
                project,
                review,
                testing,
                patch,
                git,
                pr,
                ci,
                null,
                patchHash,
                reviewId,
                testingId,
                patchId,
                gitId,
                prId,
                ciId,
                null,
                false,
                false);
    }
}
