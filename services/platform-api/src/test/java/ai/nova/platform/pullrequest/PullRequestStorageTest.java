package ai.nova.platform.pullrequest;

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
import ai.nova.platform.pullrequest.dto.PullRequestDtos.TimelineEvent;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.entity.RemotePushStatus;
import ai.nova.platform.pullrequest.provider.ProviderPullRequest;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.repository.PullRequestOperationRepository;
import ai.nova.platform.pullrequest.repository.PullRequestRecordRepository;
import ai.nova.platform.pullrequest.repository.RemotePushRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService.ResolvedRepositoryConfig;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestStorageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PullRequestStorageService storageService;

    @Autowired
    private PullRequestOperationRepository operationRepository;

    @Autowired
    private RemotePushRepository remotePushRepository;

    @Autowired
    private PullRequestRecordRepository recordRepository;

    @Autowired
    private ProjectRepositoryConfigService repositoryConfigService;

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
        user = PullRequestTestFixture.adminUser();
        bareRemoteParent = Path.of(System.getProperty("java.io.tmpdir"), "nova-pr-storage-tests");
        Files.createDirectories(bareRemoteParent);
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);
    }

    @Test
    void pendingThenSucceededPersistsPushAndRecord() throws Exception {
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
                "pr-store-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        ResolvedRepositoryConfig config = repositoryConfigService.resolve(
                PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID);
        Instant now = Instant.now();
        UUID operationId = UUID.randomUUID();
        String branch = gitOperation.branchName();

        PullRequestOperation pending = storageService.startPending(
                operationId,
                task,
                gitOperation.id(),
                gitOperation.patchResultId(),
                config,
                branch,
                gitOperation.commitHash(),
                gitOperation.patchHash(),
                now,
                List.of(new TimelineEvent("STARTED", now, "start")));
        assertThat(pending.status()).isEqualTo(PullRequestStatus.PENDING);

        storageService.updateStatus(operationId, PullRequestStatus.PUSHING, List.of());
        storageService.markPushed(
                operationId,
                gitOperation.commitHash(),
                "origin",
                branch,
                gitOperation.commitHash(),
                RemotePushStatus.SUCCEEDED,
                now,
                now,
                List.of());

        PullRequestOperation succeeded = storageService.markSucceeded(
                operationId,
                new ProviderPullRequest(
                        "1",
                        1L,
                        "memory://ghurm-nova-platform/nova-demo/pull/1",
                        "Test PR",
                        branch,
                        "main",
                        "open",
                        gitOperation.commitHash()),
                gitOperation.commitHash(),
                now,
                List.of());

        assertThat(succeeded.status()).isEqualTo(PullRequestStatus.SUCCEEDED);
        assertThat(succeeded.errorCode()).isNull();
        assertThat(succeeded.pullRequestNumber()).isEqualTo(1L);
        assertThat(operationRepository.findById(operationId)).isPresent();
        assertThat(remotePushRepository.findFirstByPullRequestOperationIdOrderByStartedAtDesc(operationId))
                .isPresent();
        assertThat(recordRepository.findFirstByPullRequestOperationIdOrderByCreatedAtDesc(operationId))
                .isPresent();
        assertThat(storageService.findLatest(gitOperation.taskId(), task.getOrganizationId()).status())
                .isEqualTo(PullRequestStatus.SUCCEEDED);
    }

    @Test
    void markFailedPersistsErrorCodeWithoutRecord() throws Exception {
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
                "pr-store-fail-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        Instant now = Instant.now();
        UUID operationId = UUID.randomUUID();

        storageService.startPending(
                operationId,
                task,
                gitOperation.id(),
                gitOperation.patchResultId(),
                repositoryConfigService.resolve(
                        PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID),
                gitOperation.branchName(),
                gitOperation.commitHash(),
                gitOperation.patchHash(),
                now,
                List.of());

        PullRequestOperation failed = storageService.markFailed(
                operationId, "PR_PUSH_FAILED", "push failed", now, List.of());
        assertThat(failed.status()).isEqualTo(PullRequestStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("PR_PUSH_FAILED");
        assertThat(recordRepository.findFirstByPullRequestOperationIdOrderByCreatedAtDesc(operationId))
                .isEmpty();
    }
}
