package ai.nova.platform.git;

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
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.revwalk.RevCommit;
import org.eclipse.jgit.revwalk.RevWalk;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.dto.GitDtos.GitRunRequest;
import ai.nova.platform.git.entity.GitStatus;
import ai.nova.platform.git.repository.GitBranchRepository;
import ai.nova.platform.git.repository.GitCommitRepository;
import ai.nova.platform.git.repository.GitOperationRepository;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.git.support.TestGitSourceFixture;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class GitIntegrationAgentServiceTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final UUID PROJECT_UUID = UUID.fromString(PROJECT_ID);
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");
    private static final String VALID_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class LoginService {}
            +// validated
            """;
    /** Valid unified diff that fails JGit apply due to context mismatch. */
    private static final String BAD_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class WrongContent {}
            +// fail
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private GitOperationRepository operationRepository;

    @Autowired
    private GitBranchRepository branchRepository;

    @Autowired
    private GitCommitRepository commitRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());

        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@nova.local","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        accessToken = objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
        user = new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("GIT_RUN", "GIT_READ"),
                true);

        assertThat(gitProperties.isAllowInitRepository()).isFalse();
        TestGitSourceFixture.ensureSourceRepository(
                controlledGitService,
                ORG_ID,
                PROJECT_UUID,
                gitProperties.getBaseRef(),
                TestGitSourceFixture.loginServiceSeeds());
    }

    @Test
    void runCreatesBranchAppliesPatchAndCommits() throws Exception {
        UUID taskId = createTaskWithApprovedPatch("git-svc-" + UUID.randomUUID(), VALID_PATCH);

        GitOperation result = gitAgentService.run(new GitRunRequest(taskId), user);
        assertThat(result.status()).isEqualTo(GitStatus.SUCCEEDED);
        assertThat(result.branchName()).isEqualTo("ai/task-" + taskId);
        assertThat(result.commitHash()).isNotBlank();
        assertThat(result.patchHash()).hasSize(64);
        assertThat(result.errorCode()).isNull();
        assertThat(result.validation().valid()).isTrue();
        assertThat(result.commits()).hasSize(1);
        assertThat(result.commits().get(0).message()).contains(taskId.toString());
        assertThat(result.timeline()).isNotEmpty();
        assertThat(result.repositoryPath()).contains("operations");
        assertThat(result.repositoryPath()).contains(result.id().toString());

        assertCommitParentMatchesBaseRef(Path.of(result.repositoryPath()), result.commitHash(), result.baseRef());
    }

    @Test
    void runRejectsWhenPatchMissing() throws Exception {
        UUID taskId = createTask("git-nopatch-" + UUID.randomUUID());
        assertThatThrownBy(() -> gitAgentService.run(new GitRunRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_PATCH_NOT_APPROVED");
    }

    @Test
    void runRejectsWhenBranchAlreadyExists() throws Exception {
        UUID taskId = createTaskWithApprovedPatch("git-dup-" + UUID.randomUUID(), VALID_PATCH);
        gitAgentService.run(new GitRunRequest(taskId), user);
        assertThatThrownBy(() -> gitAgentService.run(new GitRunRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_BRANCH_EXISTS");
    }

    @Test
    void runRejectsMissingSourceRepository() throws Exception {
        UUID orphanOrg = UUID.fromString("11111111-1111-1111-1111-111111111199");
        Path missing = controlledGitService.resolveSourceRepositoryPath(orphanOrg, PROJECT_UUID);
        assertThat(Files.exists(missing.resolve(".git"))).isFalse();

        assertThatThrownBy(() -> controlledGitService.requireSourceRepository(missing, "main"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_REPO_MISSING");
    }

    @Test
    void runRejectsMissingBaseRefWithoutFallingBackToHead() throws Exception {
        String original = gitProperties.getBaseRef();
        gitProperties.setBaseRef("refs/heads/does-not-exist-" + UUID.randomUUID());
        try {
            UUID taskId = createTaskWithApprovedPatch("git-badbase-" + UUID.randomUUID(), VALID_PATCH);
            assertThatThrownBy(() -> gitAgentService.run(new GitRunRequest(taskId), user))
                    .isInstanceOf(ApiException.class)
                    .extracting(ex -> ((ApiException) ex).getCode())
                    .isEqualTo("GIT_INVALID_BASE");
            assertThat(operationRepository.findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(taskId, ORG_ID))
                    .isEmpty();
        } finally {
            gitProperties.setBaseRef(original);
        }
    }

    @Test
    void syntheticRepositoryInitializationIsDisabled() {
        assertThat(gitProperties.isAllowInitRepository()).isFalse();
    }

    @Test
    void failedApplyPersistsFailedWithoutBranchOrCommitRows() throws Exception {
        UUID taskId = createTaskWithApprovedPatch("git-fail-" + UUID.randomUUID(), BAD_PATCH);
        assertThatThrownBy(() -> gitAgentService.run(new GitRunRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("GIT_APPLY_FAILED");

        GitOperation failed = gitAgentService.getLatest(taskId, user);
        assertThat(failed.status()).isEqualTo(GitStatus.FAILED);
        assertThat(failed.errorCode()).isEqualTo("GIT_APPLY_FAILED");
        assertThat(failed.commitHash()).isNull();
        assertThat(branchRepository.findByGitOperationIdOrderByCreatedAtAsc(failed.id())).isEmpty();
        assertThat(commitRepository.findByGitOperationIdOrderByCreatedAtAsc(failed.id())).isEmpty();
        assertThat(Files.isDirectory(Path.of(failed.repositoryPath()))).isTrue();
    }

    @Test
    void dirtySourceDoesNotContaminateOperationWorkspace() throws Exception {
        Path source = controlledGitService.resolveSourceRepositoryPath(ORG_ID, PROJECT_UUID);
        Path dirt = source.resolve("UNTRACKED_DIRTY.txt");
        Files.writeString(dirt, "should-not-clone\n", StandardCharsets.UTF_8);

        UUID taskId = createTaskWithApprovedPatch("git-dirty-" + UUID.randomUUID(), VALID_PATCH);
        GitOperation result = gitAgentService.run(new GitRunRequest(taskId), user);
        assertThat(result.status()).isEqualTo(GitStatus.SUCCEEDED);
        assertThat(Files.exists(Path.of(result.repositoryPath()).resolve("UNTRACKED_DIRTY.txt"))).isFalse();
    }

    @Test
    void concurrentOperationsUseIsolatedWorkspaces() throws Exception {
        UUID taskA = createTaskWithApprovedPatch("git-conc-a-" + UUID.randomUUID(), VALID_PATCH);
        UUID taskB = createTaskWithApprovedPatch("git-conc-b-" + UUID.randomUUID(), VALID_PATCH);

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Callable<GitOperation> runA = () -> gitAgentService.run(new GitRunRequest(taskA), user);
            Callable<GitOperation> runB = () -> gitAgentService.run(new GitRunRequest(taskB), user);
            Future<GitOperation> futureA = pool.submit(runA);
            Future<GitOperation> futureB = pool.submit(runB);
            GitOperation a = futureA.get(120, TimeUnit.SECONDS);
            GitOperation b = futureB.get(120, TimeUnit.SECONDS);

            assertThat(a.status()).isEqualTo(GitStatus.SUCCEEDED);
            assertThat(b.status()).isEqualTo(GitStatus.SUCCEEDED);
            assertThat(a.repositoryPath()).isNotEqualTo(b.repositoryPath());
            assertThat(a.id()).isNotEqualTo(b.id());
            assertThat(a.commitHash()).isNotEqualTo(b.commitHash());

            Path source = controlledGitService.resolveSourceRepositoryPath(ORG_ID, PROJECT_UUID);
            try (Repository repository = new FileRepositoryBuilder()
                    .setGitDir(source.resolve(".git").toFile())
                    .setMustExist(true)
                    .build()) {
                assertThat(repository.getBranch()).isEqualTo(gitProperties.getBaseRef());
                assertThat(repository.findRef(Constants.R_HEADS + "ai/task-" + taskA)).isNull();
                assertThat(repository.findRef(Constants.R_HEADS + "ai/task-" + taskB)).isNull();
            }
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void patchFailureDoesNotAffectOtherOperation() throws Exception {
        UUID goodTask = createTaskWithApprovedPatch("git-iso-ok-" + UUID.randomUUID(), VALID_PATCH);
        UUID badTask = createTaskWithApprovedPatch("git-iso-bad-" + UUID.randomUUID(), BAD_PATCH);

        GitOperation good = gitAgentService.run(new GitRunRequest(goodTask), user);
        assertThat(good.status()).isEqualTo(GitStatus.SUCCEEDED);

        assertThatThrownBy(() -> gitAgentService.run(new GitRunRequest(badTask), user))
                .isInstanceOf(ApiException.class);

        GitOperation stillGood = gitAgentService.getLatest(goodTask, user);
        assertThat(stillGood.status()).isEqualTo(GitStatus.SUCCEEDED);
        assertThat(stillGood.commitHash()).isEqualTo(good.commitHash());
        assertThat(Files.exists(Path.of(good.repositoryPath()).resolve("src/LoginService.java"))).isTrue();
    }

    private void assertCommitParentMatchesBaseRef(Path repoPath, String commitHash, String baseRef)
            throws Exception {
        try (Repository repository = new FileRepositoryBuilder()
                        .setGitDir(repoPath.resolve(".git").toFile())
                        .setMustExist(true)
                        .build();
                RevWalk walk = new RevWalk(repository)) {
            ObjectId base = repository.resolve(baseRef);
            assertThat(base).isNotNull();
            RevCommit commit = walk.parseCommit(ObjectId.fromString(commitHash));
            assertThat(commit.getParentCount()).isGreaterThanOrEqualTo(1);
            assertThat(commit.getParent(0).getId()).isEqualTo(base);
        }
    }

    private UUID createTaskWithApprovedPatch(String name, String patchContent) throws Exception {
        UUID taskId = createTask(name);
        var task = taskRepository.findById(taskId).orElseThrow();
        artifactStorageService.replaceArtifacts(
                task,
                List.of(new GeneratedArtifactDraft(
                        ArtifactType.SOURCE_FILE,
                        ArtifactLanguage.JAVA,
                        "src/LoginService.java",
                        "LoginService.java",
                        "class LoginService {}")),
                10L,
                "coding-local",
                "LOCAL",
                5L);
        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(taskId, task.getOrganizationId());
        ParsedPatchOutput parsed =
                new ParsedPatchOutput("Generated patch", 1, 1, 0, patchContent, PatchStatus.VALID);
        patchStorageService.replaceResult(
                task,
                artifacts,
                parsed,
                patchDiffParser.parseAndValidate(patchContent),
                5L,
                "patch-local",
                "LOCAL",
                3L);
        return taskId;
    }

    private UUID createTask(String name) throws Exception {
        MvcResult runResult = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"%s",
                                  "objective":"Build login",
                                  "executionMode":"SEQUENTIAL",
                                  "failurePolicy":"FAIL_FAST",
                                  "maxParallelTasks":1,
                                  "maximumDurationMs":60000
                                }
                                """.formatted(PROJECT_ID, name)))
                .andExpect(status().isCreated())
                .andReturn();
        String runId = objectMapper.readTree(runResult.getResponse().getContentAsString()).get("id").asText();
        MvcResult taskResult = mockMvc.perform(post("/api/orchestration-runs/" + runId + "/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskKey":"coding-1",
                                  "displayName":"Generate login",
                                  "description":"agentRole=coding",
                                  "taskType":"AGENT_TURN",
                                  "modelReference":"git-local",
                                  "maxAttempts":1,
                                  "retryBackoffMs":0,
                                  "priority":1,
                                  "timeoutSeconds":60,
                                  "idempotencyKey":"coding-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode task = objectMapper.readTree(taskResult.getResponse().getContentAsString());
        return UUID.fromString(task.get("id").asText());
    }
}
