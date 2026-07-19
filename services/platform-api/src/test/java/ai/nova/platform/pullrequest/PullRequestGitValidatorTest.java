package ai.nova.platform.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.charset.StandardCharsets;
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
import ai.nova.platform.git.dto.GitDtos.GitApplyResult;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.dto.GitDtos.GitValidation;
import ai.nova.platform.git.entity.GitStatus;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.service.PullRequestGitValidator;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestGitValidatorTest {

    @Autowired
    private PullRequestGitValidator validator;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private GitProperties gitProperties;

    @Autowired
    private GitIntegrationAgentService gitAgentService;

    @Autowired
    private ControlledGitService controlledGitService;

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

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());

        accessToken = PullRequestTestFixture.loginAdmin(mockMvc, objectMapper);
        user = PullRequestTestFixture.adminUser();
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
    }

    @Test
    void rejectsMissingWorkspace() throws Exception {
        UUID taskId = PullRequestTestFixture.createTask(mockMvc, objectMapper, accessToken, "pr-val-missing-" + UUID.randomUUID());
        AgentOrchestrationTask task = taskRepository.findById(taskId).orElseThrow();
        GitOperation gitOperation = fakeGitOperation(
                task,
                GitStatus.SUCCEEDED,
                "ai/task-" + taskId,
                "abc1234567890123456789012345678901234567890",
                Path.of(System.getProperty("java.io.tmpdir"), "missing-" + UUID.randomUUID()).toString());

        assertThatThrownBy(() -> validator.validateWorkspace(gitOperation, task, null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_GIT_WORKSPACE_MISSING");
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
                "pr-val-dirty-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        Files.writeString(
                Path.of(gitOperation.repositoryPath()).resolve("DIRTY.txt"),
                "x\n",
                StandardCharsets.UTF_8);

        assertThatThrownBy(() -> validator.validateWorkspace(gitOperation, task, null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_GIT_WORKSPACE_DIRTY");
    }

    @Test
    void rejectsHeadMismatch() throws Exception {
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
                "pr-val-head-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        GitOperation mismatched = new GitOperation(
                gitOperation.id(),
                gitOperation.taskId(),
                gitOperation.runId(),
                gitOperation.projectId(),
                gitOperation.patchResultId(),
                gitOperation.status(),
                gitOperation.branchName(),
                "0000000000000000000000000000000000000000",
                gitOperation.patchHash(),
                gitOperation.repositoryPath(),
                gitOperation.baseRef(),
                null,
                gitOperation.validation(),
                gitOperation.applyResult(),
                gitOperation.branches(),
                gitOperation.commits(),
                gitOperation.timeline(),
                gitOperation.startedAt(),
                gitOperation.completedAt(),
                gitOperation.createdAt());

        assertThatThrownBy(() -> validator.validateWorkspace(mismatched, task, null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_LOCAL_COMMIT_MISMATCH");
    }

    @Test
    void rejectsBranchMismatch() throws Exception {
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
                "pr-val-branch-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        GitOperation mismatched = new GitOperation(
                gitOperation.id(),
                gitOperation.taskId(),
                gitOperation.runId(),
                gitOperation.projectId(),
                gitOperation.patchResultId(),
                gitOperation.status(),
                "wrong-branch",
                gitOperation.commitHash(),
                gitOperation.patchHash(),
                gitOperation.repositoryPath(),
                gitOperation.baseRef(),
                null,
                gitOperation.validation(),
                gitOperation.applyResult(),
                gitOperation.branches(),
                gitOperation.commits(),
                gitOperation.timeline(),
                gitOperation.startedAt(),
                gitOperation.completedAt(),
                gitOperation.createdAt());

        assertThatThrownBy(() -> validator.validateWorkspace(mismatched, task, null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isIn("PR_LOCAL_BRANCH_MISMATCH", "PR_LOCAL_COMMIT_MISMATCH");
    }

    @Test
    void rejectsPathOutsideWorkspaceRoot() throws Exception {
        UUID taskId = PullRequestTestFixture.createTask(mockMvc, objectMapper, accessToken, "pr-val-outside-" + UUID.randomUUID());
        AgentOrchestrationTask task = taskRepository.findById(taskId).orElseThrow();
        GitOperation gitOperation = fakeGitOperation(
                task,
                GitStatus.SUCCEEDED,
                "ai/task-" + taskId,
                "abc1234567890123456789012345678901234567890",
                "/tmp/outside-root-" + UUID.randomUUID());

        assertThatThrownBy(() -> validator.validateWorkspace(gitOperation, task, null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("PR_GIT_WORKSPACE_MISSING");
    }

    @Test
    void acceptsValidWorkspace() throws Exception {
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
                "pr-val-ok-");
        AgentOrchestrationTask task = taskRepository.findById(gitOperation.taskId()).orElseThrow();
        PatchResult patch = patchStorageService.findLatest(task.getId(), task.getOrganizationId());

        validator.validateWorkspace(gitOperation, task, patch);
        assertThat(gitOperation.status()).isEqualTo(GitStatus.SUCCEEDED);
    }

    private static GitOperation fakeGitOperation(
            AgentOrchestrationTask task,
            GitStatus status,
            String branch,
            String commitHash,
            String repositoryPath) {
        Instant now = Instant.now();
        return new GitOperation(
                UUID.randomUUID(),
                task.getId(),
                task.getRunId(),
                task.getProjectId(),
                UUID.randomUUID(),
                status,
                branch,
                commitHash,
                "a".repeat(64),
                repositoryPath,
                "main",
                null,
                new GitValidation(true, "ok"),
                new GitApplyResult(true, "ok"),
                List.of(),
                List.of(),
                List.of(),
                now,
                now,
                now);
    }
}
