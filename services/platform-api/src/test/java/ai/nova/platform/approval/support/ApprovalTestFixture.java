package ai.nova.platform.approval.support;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.ci.dto.CiDtos.TimelineEvent;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.provider.InMemoryCiProvider;
import ai.nova.platform.ci.provider.ProviderWorkflowRun;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.dto.TestingDtos.ParsedTestingOutput;
import ai.nova.platform.testing.service.TestingStorageService;

public final class ApprovalTestFixture {

    public static final String PROJECT_ID = PullRequestTestFixture.PROJECT_ID;
    public static final UUID ORG_ID = PullRequestTestFixture.ORG_ID;
    public static final UUID USER_ID = PullRequestTestFixture.USER_ID;
    public static final UUID DEFAULT_POLICY_ID = UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaa0001");

    private ApprovalTestFixture() {
    }

    public static AuthenticatedUser adminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of(
                        "APPROVAL_GATE_RUN",
                        "APPROVAL_GATE_READ",
                        "APPROVAL_GATE_APPROVE",
                        "APPROVAL_GATE_REJECT",
                        "APPROVAL_POLICY_READ",
                        "APPROVAL_POLICY_MANAGE"),
                true);
    }

    public static AuthenticatedUser readOnlyUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "viewer@nova.local",
                "Viewer",
                List.of("USER"),
                List.of("APPROVAL_GATE_READ"),
                true);
    }

    public static void allowAuthorApproval(ApprovalPolicyRepository policyRepository) {
        policyRepository.findById(DEFAULT_POLICY_ID).ifPresent(policy -> {
            policy.setProhibitAuthorApproval(false);
            policyRepository.save(policy);
        });
    }

    public static void setRequiredHumanApprovals(ApprovalPolicyRepository policyRepository, int count) {
        policyRepository.findById(DEFAULT_POLICY_ID).ifPresent(policy -> {
            policy.setRequiredHumanApprovals(count);
            policyRepository.save(policy);
        });
    }

    public static UUID seedTaskWithFullPipeline(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String accessToken,
            AuthenticatedUser user,
            GitIntegrationAgentService gitAgentService,
            ArtifactStorageService artifactStorageService,
            PatchStorageService patchStorageService,
            PatchDiffParser patchDiffParser,
            ReviewStorageService reviewStorageService,
            TestingStorageService testingStorageService,
            PullRequestStorageService pullRequestStorageService,
            ProjectRepositoryConfigService repositoryConfigService,
            CiStorageService ciStorageService,
            ControlledGitService controlledGitService,
            GitProperties gitProperties,
            ProjectRepositoryConfigRepository configRepository,
            AgentOrchestrationTaskRepository taskRepository,
            Path bareRemoteParent,
            String namePrefix) throws Exception {
        Files.createDirectories(bareRemoteParent);
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(configRepository, ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);

        GitOperation gitOperation = PullRequestTestFixture.createSuccessfulGitOperationViaAgent(
                mockMvc,
                objectMapper,
                accessToken,
                user,
                gitAgentService,
                artifactStorageService,
                patchStorageService,
                patchDiffParser,
                taskRepository,
                namePrefix);
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(task.getId(), task.getOrganizationId());

        reviewStorageService.replaceReview(
                task,
                artifacts,
                new ParsedReviewOutput("Approved review", 85, true, List.of()),
                2L,
                "review-local",
                "LOCAL",
                3L);

        testingStorageService.replaceResult(
                task,
                artifacts,
                new ParsedTestingOutput("All tests generated", 80, List.of()),
                2L,
                "testing-local",
                "LOCAL",
                3L);

        var prOp = ai.nova.platform.ci.support.CiTestFixture.seedSucceededPullRequestOperation(
                pullRequestStorageService, repositoryConfigService, task, gitOperation, 42L);
        var config = repositoryConfigService.resolve(task.getOrganizationId(), task.getProjectId());
        Instant now = Instant.now();
        UUID ciOperationId = UUID.randomUUID();
        ciStorageService.startPending(ciOperationId, task, prOp, config, now, List.of(new TimelineEvent("STARTED", now, "start")));
        ProviderWorkflowRun run = InMemoryCiProvider.sampleRun(
                "run-approval",
                "CI",
                gitOperation.branchName(),
                gitOperation.commitHash(),
                42L,
                "completed",
                "success");
        var job = InMemoryCiProvider.sampleJob(
                "job-approval", "build", "success", List.of(InMemoryCiProvider.sampleStep(1, "compile", "success")));
        ciStorageService.markSucceeded(
                ciOperationId,
                CiOverallStatus.SUCCESS,
                null,
                "CI passed",
                null,
                List.of(run),
                List.of(List.of(job)),
                now,
                List.of());
        return task.getId();
    }
}
