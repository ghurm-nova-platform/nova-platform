package ai.nova.platform.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.file.Files;
import java.nio.file.Path;

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
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRunRequest;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.entity.RemotePushStatus;
import ai.nova.platform.pullrequest.provider.CreatePullRequestRequest;
import ai.nova.platform.pullrequest.provider.InMemoryPullRequestProvider;
import ai.nova.platform.pullrequest.provider.ProviderPullRequest;
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.PullRequestAgentService;
import ai.nova.platform.pullrequest.service.PullRequestRemoteGitService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestIdempotencyTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PullRequestAgentService pullRequestAgentService;

    @Autowired
    private PullRequestRemoteGitService remoteGitService;

    @Autowired
    private GitIntegrationAgentService gitAgentService;

    @Autowired
    private ControlledGitService controlledGitService;

    @Autowired
    private GitProperties gitProperties;

    @Autowired
    private ProjectRepositoryConfigRepository configRepository;

    @Autowired
    private InMemoryPullRequestProvider inMemoryProvider;

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
        bareRemoteParent = Path.of(System.getProperty("java.io.tmpdir"), "nova-pr-idem-tests");
        Files.createDirectories(bareRemoteParent);
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
    }

    @Test
    void reusesExistingPullRequestWhenRemoteBranchMatchesExpectedCommit() throws Exception {
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
                "pr-idem-remote-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);

        remoteGitService.pushExactBranch(
                Path.of(gitOperation.repositoryPath()),
                bareRemote.toUri().toString(),
                gitOperation.branchName(),
                gitOperation.commitHash(),
                null);

        RepositoryRef ref = new RepositoryRef("localhost", "ghurm-nova-platform", "nova-demo");
        inMemoryProvider.recordRemoteBranch(ref, gitOperation.branchName(), gitOperation.commitHash());
        ProviderPullRequest seeded = inMemoryProvider.createPullRequest(
                new CreatePullRequestRequest(
                        ref,
                        "Existing PR",
                        "Already open",
                        gitOperation.branchName(),
                        "main",
                        true),
                null);
        assertThat(seeded.headSha()).isEqualToIgnoringCase(gitOperation.commitHash());

        PullRequestOperation result = pullRequestAgentService.run(
                new PullRequestRunRequest(gitOperation.taskId()), user);
        assertThat(result.status()).isEqualTo(PullRequestStatus.SUCCEEDED);
        assertThat(result.pullRequestNumber()).isEqualTo(seeded.number());
        assertThat(result.remotePush()).isNotNull();
        assertThat(result.remotePush().status()).isEqualTo(RemotePushStatus.SKIPPED);
    }
}
