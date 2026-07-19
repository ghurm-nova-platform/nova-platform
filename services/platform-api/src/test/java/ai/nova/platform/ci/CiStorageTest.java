package ai.nova.platform.ci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
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
import ai.nova.platform.ci.dto.CiDtos.CiObservationOperation;
import ai.nova.platform.ci.dto.CiDtos.TimelineEvent;
import ai.nova.platform.ci.entity.CiObservationStatus;
import ai.nova.platform.ci.entity.CiOverallStatus;
import ai.nova.platform.ci.provider.InMemoryCiProvider;
import ai.nova.platform.ci.provider.ProviderWorkflowRun;
import ai.nova.platform.ci.repository.CiJobRepository;
import ai.nova.platform.ci.repository.CiObservationOperationRepository;
import ai.nova.platform.ci.repository.CiStepRepository;
import ai.nova.platform.ci.repository.CiWorkflowRunRepository;
import ai.nova.platform.ci.service.CiStorageService;
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
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest
@AutoConfigureMockMvc
class CiStorageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CiStorageService storageService;

    @Autowired
    private CiObservationOperationRepository operationRepository;

    @Autowired
    private CiWorkflowRunRepository workflowRunRepository;

    @Autowired
    private CiJobRepository jobRepository;

    @Autowired
    private CiStepRepository stepRepository;

    @Autowired
    private ProjectRepositoryConfigService repositoryConfigService;

    @Autowired
    private PullRequestStorageService pullRequestStorageService;

    @Autowired
    private ProjectRepositoryConfigRepository configRepository;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private GitIntegrationAgentService gitAgentService;

    @Autowired
    private ControlledGitService controlledGitService;

    @Autowired
    private GitProperties gitProperties;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private PatchStorageService patchStorageService;

    @Autowired
    private PatchDiffParser patchDiffParser;

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
        bareRemoteParent = Path.of(System.getProperty("java.io.tmpdir"), "nova-ci-storage-tests");
        Files.createDirectories(bareRemoteParent);
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);
    }

    @Test
    void pendingThenSucceededPersistsWorkflowsJobsSteps() throws Exception {
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
                "ci-store-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        var prOp = CiTestFixture.seedSucceededPullRequestOperation(
                pullRequestStorageService, repositoryConfigService, task, gitOperation, 7L);
        var config = repositoryConfigService.resolve(task.getOrganizationId(), task.getProjectId());
        Instant now = Instant.now();
        UUID operationId = UUID.randomUUID();

        CiObservationOperation pending = storageService.startPending(
                operationId, task, prOp, config, now, List.of(new TimelineEvent("STARTED", now, "start")));
        assertThat(pending.status()).isEqualTo(CiObservationStatus.PENDING);

        storageService.updateStatus(operationId, CiObservationStatus.FETCHING, List.of());
        storageService.updateStatus(operationId, CiObservationStatus.PROCESSING, List.of());

        ProviderWorkflowRun run = InMemoryCiProvider.sampleRun(
                "run-store",
                "CI",
                gitOperation.branchName(),
                gitOperation.commitHash(),
                7L,
                "completed",
                "success");
        var job = InMemoryCiProvider.sampleJob(
                "job-store",
                "build",
                "success",
                List.of(InMemoryCiProvider.sampleStep(1, "compile", "success")));

        CiObservationOperation succeeded = storageService.markSucceeded(
                operationId,
                CiOverallStatus.SUCCESS,
                null,
                "CI passed",
                null,
                List.of(run),
                List.of(List.of(job)),
                now,
                List.of());

        assertThat(succeeded.status()).isEqualTo(CiObservationStatus.SUCCEEDED);
        assertThat(succeeded.workflows()).hasSize(1);
        assertThat(succeeded.workflows().get(0).jobs()).hasSize(1);
        assertThat(succeeded.workflows().get(0).jobs().get(0).steps()).hasSize(1);
        assertThat(operationRepository.findById(operationId)).isPresent();
        assertThat(workflowRunRepository.findByCiObservationOperationIdOrderByCreatedAtAsc(operationId))
                .hasSize(1);
        UUID runId = workflowRunRepository
                .findByCiObservationOperationIdOrderByCreatedAtAsc(operationId)
                .get(0)
                .getId();
        assertThat(jobRepository.findByCiWorkflowRunIdOrderByCreatedAtAsc(runId)).hasSize(1);
        UUID jobId = jobRepository.findByCiWorkflowRunIdOrderByCreatedAtAsc(runId).get(0).getId();
        assertThat(stepRepository.findByCiJobIdOrderByStepNumberAsc(jobId)).hasSize(1);
        assertThat(storageService.findLatest(gitOperation.taskId(), task.getOrganizationId()).status())
                .isEqualTo(CiObservationStatus.SUCCEEDED);
    }

    @Test
    void markFailedPersistsErrorCode() throws Exception {
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
                "ci-store-fail-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        var prOp = CiTestFixture.seedSucceededPullRequestOperation(
                pullRequestStorageService, repositoryConfigService, task, gitOperation, 8L);
        var config = repositoryConfigService.resolve(task.getOrganizationId(), task.getProjectId());
        Instant now = Instant.now();
        UUID operationId = UUID.randomUUID();

        storageService.startPending(operationId, task, prOp, config, now, List.of());
        CiObservationOperation failed =
                storageService.markFailed(operationId, "CI_FETCH_FAILED", "fetch failed", now, List.of());

        assertThat(failed.status()).isEqualTo(CiObservationStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("CI_FETCH_FAILED");
        assertThat(workflowRunRepository.findByCiObservationOperationIdOrderByCreatedAtAsc(operationId))
                .isEmpty();
    }
}
