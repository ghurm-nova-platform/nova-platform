package ai.nova.platform.ci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.ci.config.CiObservationProperties;
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.dto.CiDtos.CiRunRequest;
import ai.nova.platform.ci.entity.CiObservationStatus;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.provider.InMemoryCiProvider;
import ai.nova.platform.ci.provider.ProviderWorkflowRun;
import ai.nova.platform.ci.service.CiObservationAgentService;
import ai.nova.platform.ci.support.CiTestFixture;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class CiObservationAgentServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CiObservationAgentService ciObservationAgentService;

    @Autowired
    private CiObservationProperties ciObservationProperties;

    @Autowired
    private InMemoryCiProvider inMemoryCiProvider;

    @Autowired
    private GitIntegrationAgentService gitAgentService;

    @Autowired
    private ControlledGitService controlledGitService;

    @Autowired
    private GitProperties gitProperties;

    @Autowired
    private ProjectRepositoryConfigRepository configRepository;

    @Autowired
    private ProjectRepositoryConfigService repositoryConfigService;

    @Autowired
    private PullRequestStorageService pullRequestStorageService;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private PatchStorageService patchStorageService;

    @Autowired
    private PatchDiffParser patchDiffParser;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;
    private AuthenticatedUser user;
    private Path bareRemoteParent;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());

        accessToken = PullRequestTestFixture.loginAdmin(mockMvc, objectMapper);
        user = CiTestFixture.adminUser();
        bareRemoteParent = Path.of(System.getProperty("java.io.tmpdir"), "nova-ci-agent-tests");
        Files.createDirectories(bareRemoteParent);
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);

        inMemoryCiProvider.clear();
        ciObservationProperties.setEnabled(true);
    }

    @Test
    void happyPathObservesLocalCiRuns() throws Exception {
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
                "ci-happy-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        long prNumber = 42L;
        PullRequestOperation prOp = CiTestFixture.seedSucceededPullRequestOperation(
                pullRequestStorageService, repositoryConfigService, task, gitOperation, prNumber);

        RepositoryRef repoRef = new RepositoryRef("localhost", "ghurm-nova-platform", "nova-demo");
        ProviderWorkflowRun run = InMemoryCiProvider.sampleRun(
                "run-1",
                "CI",
                gitOperation.branchName(),
                gitOperation.commitHash(),
                prNumber,
                "completed",
                "success");
        inMemoryCiProvider.seedWorkflowRun(repoRef, run);
        inMemoryCiProvider.seedJobs(
                repoRef,
                "run-1",
                List.of(InMemoryCiProvider.sampleJob(
                        "job-1",
                        "build",
                        "success",
                        List.of(InMemoryCiProvider.sampleStep(1, "compile", "success")))));

        CiObservationOperation result =
                ciObservationAgentService.run(new CiRunRequest(gitOperation.taskId()), user);

        assertThat(result.status()).isEqualTo(CiObservationStatus.SUCCEEDED);
        assertThat(result.overallStatus()).isEqualTo(CiOverallStatus.SUCCESS);
        assertThat(result.pullRequestOperationId()).isEqualTo(prOp.id());
        assertThat(result.pullRequestNumber()).isEqualTo(prNumber);
        assertThat(result.workflows()).hasSize(1);
        assertThat(result.workflows().get(0).jobs()).hasSize(1);
        assertThat(result.workflows().get(0).jobs().get(0).steps()).hasSize(1);
        assertThat(result.errorCode()).isNull();
        assertThat(result.retryRecommendation()).contains("no retry needed");

        String json = objectMapper.writeValueAsString(result);
        assertThat(json.toLowerCase()).doesNotContain("token");
        assertThat(json).doesNotContain("test-token-not-real");
    }

    @Test
    void rejectsWhenFeatureDisabled() {
        ciObservationProperties.setEnabled(false);
        try {
            UUID taskId = UUID.randomUUID();
            assertThatThrownBy(() -> ciObservationAgentService.run(new CiRunRequest(taskId), user))
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getCode())
                    .isEqualTo("CI_DISABLED");
        } finally {
            ciObservationProperties.setEnabled(true);
        }
    }

    @Test
    void rejectsMissingPullRequestOperation() throws Exception {
        UUID taskId = PullRequestTestFixture.createTask(
                mockMvc, objectMapper, accessToken, "ci-nopr-" + UUID.randomUUID());
        assertThatThrownBy(() -> ciObservationAgentService.run(new CiRunRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CI_PR_OPERATION_NOT_FOUND");
    }

    @Test
    void rejectsPullRequestNotSucceeded() throws Exception {
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
                "ci-prfail-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        var config = repositoryConfigService.resolve(task.getOrganizationId(), task.getProjectId());
        UUID operationId = UUID.randomUUID();
        pullRequestStorageService.startPending(
                operationId,
                task,
                gitOperation.id(),
                gitOperation.patchResultId(),
                config,
                gitOperation.branchName(),
                gitOperation.commitHash(),
                gitOperation.patchHash(),
                java.time.Instant.now(),
                List.of());
        pullRequestStorageService.markFailed(operationId, "PR_CREATE_FAILED", "failed", java.time.Instant.now(), List.of());

        assertThatThrownBy(() -> ciObservationAgentService.run(new CiRunRequest(gitOperation.taskId()), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CI_PR_OPERATION_NOT_SUCCEEDED");
    }

    @Test
    void failedCiRunReportsFailureSummary() throws Exception {
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
                "ci-failrun-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        long prNumber = 99L;
        CiTestFixture.seedSucceededPullRequestOperation(
                pullRequestStorageService, repositoryConfigService, task, gitOperation, prNumber);

        RepositoryRef repoRef = new RepositoryRef("localhost", "ghurm-nova-platform", "nova-demo");
        ProviderWorkflowRun run = InMemoryCiProvider.sampleRun(
                "run-fail",
                "CI",
                gitOperation.branchName(),
                gitOperation.commitHash(),
                prNumber,
                "completed",
                "failure");
        inMemoryCiProvider.seedWorkflowRun(repoRef, run);
        inMemoryCiProvider.seedJobs(
                repoRef,
                "run-fail",
                List.of(InMemoryCiProvider.sampleJob(
                        "job-fail",
                        "test",
                        "failure",
                        List.of(InMemoryCiProvider.sampleStep(1, "run tests", "failure")))));

        CiObservationOperation result =
                ciObservationAgentService.run(new CiRunRequest(gitOperation.taskId()), user);

        assertThat(result.status()).isEqualTo(CiObservationStatus.SUCCEEDED);
        assertThat(result.overallStatus()).isEqualTo(CiOverallStatus.FAILED);
        assertThat(result.failureSummary()).isNotNull();
        assertThat(result.failureSummary().failedJobs()).isGreaterThan(0);
        assertThat(result.retryRecommendation()).contains("do not auto-rerun");
    }
}
