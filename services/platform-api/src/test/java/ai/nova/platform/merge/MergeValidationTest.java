package ai.nova.platform.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalDecision;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.entity.CiObservationStatus;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.entity.GitStatus;
import ai.nova.platform.git.service.GitValidator;
import ai.nova.platform.merge.config.MergeProperties;
import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.merge.entity.MergeValidationResult;
import ai.nova.platform.merge.service.MergeValidator;
import ai.nova.platform.merge.service.MergeValidator.MergeValidationContext;
import ai.nova.platform.merge.service.MergeValidator.ValidationOutcome;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.entity.TaskType;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRecord;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestValidation;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;

class MergeValidationTest {

    private static final UUID TASK_ID = UUID.randomUUID();
    private static final UUID PROJECT_ID = UUID.randomUUID();
    private static final UUID PATCH_ID = UUID.randomUUID();
    private static final UUID GIT_ID = UUID.randomUUID();
    private static final UUID PR_ID = UUID.randomUUID();
    private static final UUID CI_ID = UUID.randomUUID();
    private static final UUID APPROVAL_ID = UUID.randomUUID();
    private static final String PATCH_CONTENT = "--- a/x\n+++ b/x\n+line\n";
    private static final String PATCH_HASH = "abc123patchhash012345678901234567890123456789012345678901234";
    private static final String COMMIT_HASH = "def456commithash0123456789012345678901234567890123456789012";

    private GitValidator gitValidator;
    private MergeValidator validator;
    private MergeProperties properties;

    @BeforeEach
    void setUp() {
        gitValidator = mock(GitValidator.class);
        when(gitValidator.sha256(PATCH_CONTENT)).thenReturn(PATCH_HASH);
        validator = new MergeValidator(gitValidator);
        properties = new MergeProperties();
        properties.setRequireProtectedBranch(false);
        properties.setAllowedMethods(List.of("SQUASH", "MERGE", "REBASE"));
    }

    @Test
    void allChecksPassForConsistentEvidence() {
        ValidationOutcome outcome = validator.validate(buildContext(MergeMethod.SQUASH, PATCH_HASH, COMMIT_HASH, "open"));
        assertThat(outcome.passed()).isTrue();
        assertThat(outcome.checks()).extracting(c -> c.checkCode()).contains("PATCH_HASH_UNCHANGED", "CI_SUCCESS");
    }

    @Test
    void patchMismatchFailsWithCorrectCode() {
        ValidationOutcome outcome =
                validator.validate(buildContext(MergeMethod.SQUASH, "wrong-hash", COMMIT_HASH, "open"));
        assertThat(outcome.passed()).isFalse();
        assertThat(outcome.errorCode()).isEqualTo("MERGE_PATCH_MISMATCH");
        assertThat(outcome.checks()).anyMatch(c -> c.checkCode().equals("PATCH_HASH_UNCHANGED")
                && c.result() == MergeValidationResult.FAILED);
    }

    @Test
    void prNotOpenFails() {
        ValidationOutcome outcome = validator.validate(buildContext(MergeMethod.SQUASH, PATCH_HASH, COMMIT_HASH, "closed"));
        assertThat(outcome.passed()).isFalse();
        assertThat(outcome.errorCode()).isEqualTo("MERGE_PR_NOT_OPEN");
    }

    @Test
    void disallowedMergeMethodFails() {
        properties.setAllowedMethods(List.of("MERGE"));
        ValidationOutcome outcome = validator.validate(buildContext(MergeMethod.SQUASH, PATCH_HASH, COMMIT_HASH, "open"));
        assertThat(outcome.passed()).isFalse();
        assertThat(outcome.errorCode()).isEqualTo("MERGE_VALIDATION_FAILED");
    }

    private MergeValidationContext buildContext(
            MergeMethod method, String patchHash, String commitHash, String prState) {
        AgentOrchestrationTask task = new AgentOrchestrationTask(
                TASK_ID,
                UUID.randomUUID(),
                PROJECT_ID,
                UUID.randomUUID(),
                "task-key",
                "display",
                TaskType.AGENT_TURN,
                TaskStatus.READY,
                "idem",
                3,
                1000L,
                1,
                600,
                UUID.randomUUID(),
                Instant.now());

        ApprovalDecision approval = new ApprovalDecision(
                APPROVAL_ID,
                TASK_ID,
                PROJECT_ID,
                ApprovalDecisionValue.APPROVED,
                true,
                UUID.randomUUID(),
                "default",
                1,
                "evidence-fp",
                "decision-fp",
                PATCH_ID,
                patchHash,
                GIT_ID,
                commitHash,
                PR_ID,
                42L,
                "https://github.com/o/r/pull/42",
                CI_ID,
                CiOverallStatus.SUCCESS.name(),
                null,
                1,
                1,
                0,
                "approved",
                Instant.now().plusSeconds(3600),
                java.util.Map.of(),
                List.of(),
                List.of(),
                List.of(),
                List.of(),
                Instant.now(),
                Instant.now());

        PatchResult patch = new PatchResult(
                PATCH_ID,
                TASK_ID,
                UUID.randomUUID(),
                PROJECT_ID,
                "summary",
                PatchStatus.VALID,
                null,
                PATCH_CONTENT,
                List.of(),
                List.of(),
                null,
                null,
                null,
                null,
                null,
                Instant.now());

        GitOperation git = new GitOperation(
                GIT_ID,
                TASK_ID,
                UUID.randomUUID(),
                PROJECT_ID,
                PATCH_ID,
                GitStatus.SUCCEEDED,
                "feature/x",
                commitHash,
                patchHash,
                "/tmp/repo",
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

        PullRequestOperation pullRequest = new PullRequestOperation(
                PR_ID,
                TASK_ID,
                PROJECT_ID,
                GIT_ID,
                PATCH_ID,
                PullRequestStatus.SUCCEEDED,
                "LOCAL",
                "owner",
                "repo",
                "origin",
                "http://local",
                "feature/x",
                "main",
                commitHash,
                commitHash,
                patchHash,
                42L,
                "https://github.com/o/r/pull/42",
                "title",
                null,
                new PullRequestValidation(true, "ok"),
                null,
                new PullRequestRecord(
                        UUID.randomUUID(), "LOCAL", "1", 42L, "url", "title", "feature/x", "main", prState, Instant.now()),
                List.of(),
                Instant.now(),
                Instant.now(),
                Instant.now());

        CiObservationOperation ci = new CiObservationOperation(
                CI_ID,
                TASK_ID,
                PROJECT_ID,
                PR_ID,
                CiObservationStatus.SUCCEEDED,
                "LOCAL",
                "owner",
                "repo",
                "feature/x",
                "main",
                commitHash,
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

        return new MergeValidationContext(
                task, approval, patch, git, pullRequest, ci, method, properties, null, "", Instant.now());
    }
}
