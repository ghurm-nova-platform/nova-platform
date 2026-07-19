package ai.nova.platform.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
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
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.entity.GitStatus;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.config.PullRequestProperties;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestOperation;
import ai.nova.platform.pullrequest.dto.PullRequestDtos.PullRequestRunRequest;
import ai.nova.platform.pullrequest.entity.PullRequestStatus;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.PullRequestAgentService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestAgentServiceTest {

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
    private PullRequestProperties pullRequestProperties;

    @Autowired
    private ProjectRepositoryConfigRepository configRepository;

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
        user = PullRequestTestFixture.adminUser();
        bareRemoteParent = Path.of(System.getProperty("java.io.tmpdir"), "nova-pr-agent-tests");
        Files.createDirectories(bareRemoteParent);

        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
        pullRequestProperties.setEnabled(true);
    }

    @Test
    void happyPathCreatesLocalPullRequest() throws Exception {
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
                "pr-happy-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);

        PullRequestOperation result = pullRequestAgentService.run(
                new PullRequestRunRequest(gitOperation.taskId()), user);

        assertThat(result.status()).isEqualTo(PullRequestStatus.SUCCEEDED);
        assertThat(result.provider()).isEqualTo("LOCAL");
        assertThat(result.repositoryOwner()).isEqualTo("ghurm-nova-platform");
        assertThat(result.repositoryName()).isEqualTo("nova-demo");
        assertThat(result.sourceBranch()).isEqualTo(gitOperation.branchName());
        assertThat(result.targetBranch()).isEqualTo("main");
        assertThat(result.localCommitHash()).isEqualToIgnoringCase(gitOperation.commitHash());
        assertThat(result.remoteCommitHash()).isEqualToIgnoringCase(gitOperation.commitHash());
        assertThat(result.pullRequestNumber()).isNotNull();
        assertThat(result.pullRequestUrl()).startsWith("memory://");
        assertThat(result.pullRequestTitle()).isNotBlank();
        assertThat(result.errorCode()).isNull();
        assertThat(result.remotePush()).isNotNull();
        assertThat(result.pullRequestRecord()).isNotNull();
        assertThat(result.timeline()).isNotEmpty();

        String json = objectMapper.writeValueAsString(result);
        assertThat(json.toLowerCase()).doesNotContain("token");
        assertThat(json).doesNotContain("test-token-not-real");
    }

    @Test
    void rejectsWhenFeatureDisabled() {
        pullRequestProperties.setEnabled(false);
        try {
            UUID taskId = UUID.randomUUID();
            assertThatThrownBy(() -> pullRequestAgentService.run(new PullRequestRunRequest(taskId), user))
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getCode())
                    .isEqualTo("PR_DISABLED");
        } finally {
            pullRequestProperties.setEnabled(true);
        }
    }

    @Test
    void rejectsMissingGitOperation() throws Exception {
        UUID taskId = PullRequestTestFixture.createTask(mockMvc, objectMapper, accessToken, "pr-nogit-" + UUID.randomUUID());
        assertThatThrownBy(() -> pullRequestAgentService.run(new PullRequestRunRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_GIT_OPERATION_NOT_FOUND");
    }

    @Test
    void rejectsWhenGitOperationNotSucceeded() throws Exception {
        UUID taskId = PullRequestTestFixture.createTask(mockMvc, objectMapper, accessToken, "pr-gitfail-" + UUID.randomUUID());
        var task = taskRepository.findById(taskId).orElseThrow();
        artifactStorageService.replaceArtifacts(
                task,
                List.of(new ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft(
                        ai.nova.platform.coding.entity.ArtifactType.SOURCE_FILE,
                        ai.nova.platform.coding.entity.ArtifactLanguage.JAVA,
                        "src/LoginService.java",
                        "LoginService.java",
                        "class LoginService {}")),
                10L,
                "coding-local",
                "LOCAL",
                5L);
        var artifacts = artifactStorageService.listByTask(taskId, task.getOrganizationId());
        patchStorageService.replaceResult(
                task,
                artifacts,
                new ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput(
                        "Generated patch", 1, 1, 0, PullRequestTestFixture.BAD_PATCH, ai.nova.platform.patch.entity.PatchStatus.VALID),
                patchDiffParser.parseAndValidate(PullRequestTestFixture.BAD_PATCH),
                5L,
                "patch-local",
                "LOCAL",
                3L);
        assertThatThrownBy(() -> gitAgentService.run(
                        new ai.nova.platform.git.dto.GitDtos.GitRunRequest(taskId),
                        user))
                .isInstanceOf(ApiException.class);

        assertThatThrownBy(() -> pullRequestAgentService.run(new PullRequestRunRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_GIT_OPERATION_NOT_SUCCEEDED");
    }

    @Test
    void rejectsRemoteBranchConflict() throws Exception {
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
                "pr-conflict-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);

        Path scratch = bareRemoteParent.resolve("conflict-scratch-" + UUID.randomUUID());
        Files.createDirectories(scratch);
        try (Git git = Git.cloneRepository()
                .setURI(bareRemote.toUri().toString())
                .setDirectory(scratch.resolve("clone").toFile())
                .call()) {
            Files.writeString(
                    scratch.resolve("clone/conflict.txt"), "other commit\n", StandardCharsets.UTF_8);
            git.add().addFilepattern(".").call();
            PersonIdent author = new PersonIdent("Nova Test", "test@nova.local");
            git.commit()
                    .setMessage("conflicting remote commit")
                    .setAuthor(author)
                    .setCommitter(author)
                    .call();
            git.branchCreate().setName(gitOperation.branchName()).call();
            git.checkout().setName(gitOperation.branchName()).call();
            git.push().setRemote(bareRemote.toUri().toString()).add(gitOperation.branchName()).call();
        }

        assertThatThrownBy(() -> pullRequestAgentService.run(
                        new PullRequestRunRequest(gitOperation.taskId()), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_REMOTE_BRANCH_CONFLICT");
    }

    @Test
    void secondRunReturnsExistingSucceededOperation() throws Exception {
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
                "pr-idem-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);

        PullRequestOperation first = pullRequestAgentService.run(
                new PullRequestRunRequest(gitOperation.taskId()), user);
        PullRequestOperation second = pullRequestAgentService.run(
                new PullRequestRunRequest(gitOperation.taskId()), user);

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.status()).isEqualTo(PullRequestStatus.SUCCEEDED);
    }

    @Test
    void rejectsDirtyWorkspace() throws Exception {
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
                "pr-dirty-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);
        Files.writeString(
                Path.of(gitOperation.repositoryPath()).resolve("DIRTY.txt"),
                "untracked\n",
                StandardCharsets.UTF_8);

        assertThatThrownBy(() -> pullRequestAgentService.run(
                        new PullRequestRunRequest(gitOperation.taskId()), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_GIT_WORKSPACE_DIRTY");
    }

    @Test
    void failedOperationPersistsErrorCode() throws Exception {
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
                "pr-failpersist-");
        var config = configRepository
                .findByOrganizationIdAndProjectId(
                        PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID)
                .orElseThrow();
        // Allowlisted host so PENDING is created, then push fails and FAILED is persisted.
        config.setRemoteUrl("https://github.com/ghurm-nova-platform/nova-demo-missing-" + UUID.randomUUID() + ".git");
        config.setRepositoryHost("github.com");
        configRepository.save(config);

        assertThatThrownBy(() -> pullRequestAgentService.run(
                        new PullRequestRunRequest(gitOperation.taskId()), user))
                .isInstanceOf(ApiException.class);

        PullRequestOperation failed = pullRequestAgentService.getLatest(gitOperation.taskId(), user);
        assertThat(failed.status()).isEqualTo(PullRequestStatus.FAILED);
        assertThat(failed.errorCode()).isNotBlank();
        assertThat(objectMapper.writeValueAsString(failed).toLowerCase()).doesNotContain("token");
    }

    @Test
    void runViaHttpDoesNotExposeCredentials() throws Exception {
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
                "pr-http-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        PullRequestTestFixture.pointProjectRemoteTo(
                configRepository, PullRequestTestFixture.ORG_ID, PullRequestTestFixture.PROJECT_UUID, bareRemote);

        var response = mockMvc.perform(post("/api/pull-requests/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + gitOperation.taskId() + "\"}"))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(response).contains("\"status\":\"SUCCEEDED\"");
        assertThat(response.toLowerCase()).doesNotContain("token");
        assertThat(response).doesNotContain("test-token-not-real");
    }
}
