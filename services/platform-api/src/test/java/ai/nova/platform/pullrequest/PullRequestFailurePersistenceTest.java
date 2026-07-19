package ai.nova.platform.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRunRequest;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.entity.RemotePushStatus;
import ai.nova.platform.pullrequest.provider.CreatePullRequestRequest;
import ai.nova.platform.pullrequest.provider.InMemoryPullRequestProvider;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.repository.RemotePushRepository;
import ai.nova.platform.pullrequest.service.PullRequestAgentService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestFailurePersistenceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PullRequestAgentService pullRequestAgentService;

    @Autowired
    private GitIntegrationAgentService gitAgentService;

    @Autowired
    private ControlledGitService controlledGitService;

    @Autowired
    private GitProperties gitProperties;

    @Autowired
    private ProjectRepositoryConfigRepository configRepository;

    @Autowired
    private RemotePushRepository remotePushRepository;

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

    @MockitoSpyBean
    private InMemoryPullRequestProvider inMemoryProvider;

    private AuthenticatedUser user;
    private String accessToken;
    private Path bareRemoteParent;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());

        accessToken = PullRequestTestFixture.loginAdmin(mockMvc, objectMapper);
        user = PullRequestTestFixture.adminUser();
        bareRemoteParent = Path.of(System.getProperty("java.io.tmpdir"), "nova-pr-failure-tests");
        Files.createDirectories(bareRemoteParent);
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
    }

    @Test
    void pushSucceededThenProviderFailurePersistsFailedWithPushRecord() throws Exception {
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
                "pr-fail-after-push-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);

        doThrow(new ApiException(HttpStatus.BAD_GATEWAY, "PR_CREATE_FAILED", "provider blew up"))
                .when(inMemoryProvider)
                .createPullRequest(any(CreatePullRequestRequest.class), any());

        assertThatThrownBy(() -> pullRequestAgentService.run(
                        new PullRequestRunRequest(gitOperation.taskId()), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_CREATE_FAILED");

        PullRequestOperation failed = pullRequestAgentService.getLatest(gitOperation.taskId(), user);
        assertThat(failed.status()).isEqualTo(PullRequestStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("PR_CREATE_FAILED");
        assertThat(failed.remotePush()).isNotNull();
        assertThat(failed.remotePush().status()).isEqualTo(RemotePushStatus.SUCCEEDED);
        assertThat(remotePushRepository.findFirstByPullRequestOperationIdOrderByStartedAtDesc(failed.id()))
                .isPresent();
        assertThat(objectMapper.writeValueAsString(failed).toLowerCase()).doesNotContain("token");
        assertThat(objectMapper.writeValueAsString(failed)).doesNotContain("test-token-not-real");
    }
}
