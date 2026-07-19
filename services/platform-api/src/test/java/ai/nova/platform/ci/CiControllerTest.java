package ai.nova.platform.ci;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.ci.provider.InMemoryCiProvider;
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
import ai.nova.platform.pullrequest.provider.RepositoryRef;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;

@SpringBootTest
@AutoConfigureMockMvc
class CiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
        bareRemoteParent = Path.of(System.getProperty("java.io.tmpdir"), "nova-ci-controller-tests");
        Files.createDirectories(bareRemoteParent);
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);
        inMemoryCiProvider.clear();
    }

    @Test
    void runGetLatestAndHistoryViaHttp() throws Exception {
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
                "ci-http-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        long prNumber = 55L;
        CiTestFixture.seedSucceededPullRequestOperation(
                pullRequestStorageService, repositoryConfigService, task, gitOperation, prNumber);

        RepositoryRef repoRef = new RepositoryRef("localhost", "ghurm-nova-platform", "nova-demo");
        inMemoryCiProvider.seedWorkflowRun(
                repoRef,
                InMemoryCiProvider.sampleRun(
                        "run-http",
                        "CI",
                        gitOperation.branchName(),
                        gitOperation.commitHash(),
                        prNumber,
                        "completed",
                        "success"));
        inMemoryCiProvider.seedJobs(
                repoRef,
                "run-http",
                List.of(InMemoryCiProvider.sampleJob("job-http", "build", "success", List.of())));

        String runResponse = mockMvc.perform(post("/api/ci/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + gitOperation.taskId() + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(runResponse.toLowerCase()).doesNotContain("token");
        assertThat(runResponse).doesNotContain("test-token-not-real");

        mockMvc.perform(get("/api/ci/" + gitOperation.taskId()).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.workflows[0].jobs[0].jobName").value("build"));

        mockMvc.perform(get("/api/ci/" + gitOperation.taskId() + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("SUCCEEDED"));
    }
}
