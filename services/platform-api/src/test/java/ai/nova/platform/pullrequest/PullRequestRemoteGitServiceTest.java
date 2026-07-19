package ai.nova.platform.pullrequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.lib.PersonIdent;
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
import ai.nova.platform.pullrequest.service.PullRequestRemoteGitService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class PullRequestRemoteGitServiceTest {

    @Autowired
    private PullRequestRemoteGitService remoteGitService;

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
        bareRemoteParent = Path.of(System.getProperty("java.io.tmpdir"), "nova-pr-remote-git-tests");
        Files.createDirectories(bareRemoteParent);
        PullRequestTestFixture.ensureSourceRepository(controlledGitService, gitProperties);
    }

    @Test
    void pushExactBranchUsesExplicitRefspecAndVerifiesRemoteCommit() throws Exception {
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
                "pr-remote-push-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);
        Path localRepo = Path.of(gitOperation.repositoryPath());

        String remoteCommit = remoteGitService.pushExactBranch(
                localRepo,
                bareRemote.toUri().toString(),
                gitOperation.branchName(),
                gitOperation.commitHash(),
                null);

        assertThat(remoteCommit).isEqualToIgnoringCase(gitOperation.commitHash());
        assertThat(remoteGitService.findRemoteHeadCommit(
                        bareRemote.toUri().toString(), gitOperation.branchName(), null))
                .contains(remoteCommit);
        assertThat(remoteGitService.isFileRemote(bareRemote.toUri().toString())).isTrue();
    }

    @Test
    void rejectsPushWhenRemoteBranchHasDifferentCommit() throws Exception {
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
                "pr-remote-conflict-");
        Path bareRemote = PullRequestTestFixture.ensureBareRemote(bareRemoteParent);

        Path scratch = bareRemoteParent.resolve("scratch-" + UUID.randomUUID());
        Files.createDirectories(scratch);
        try (Git git = Git.cloneRepository()
                .setURI(bareRemote.toUri().toString())
                .setDirectory(scratch.resolve("clone").toFile())
                .call()) {
            Files.writeString(scratch.resolve("clone/other.txt"), "x\n", StandardCharsets.UTF_8);
            git.add().addFilepattern(".").call();
            PersonIdent author = new PersonIdent("Nova Test", "test@nova.local");
            git.commit()
                    .setMessage("other")
                    .setAuthor(author)
                    .setCommitter(author)
                    .call();
            git.branchCreate().setName(gitOperation.branchName()).call();
            git.checkout().setName(gitOperation.branchName()).call();
            git.push().setRemote(bareRemote.toUri().toString()).add(gitOperation.branchName()).call();
        }

        GitOperation otherTask = PullRequestTestFixture.createSuccessfulGitOperationViaAgent(
                mockMvc,
                objectMapper,
                accessToken,
                user,
                gitAgentService,
                artifactStorageService,
                patchStorageService,
                patchDiffParser,
                taskRepository,
                "pr-remote-other-");

        assertThatThrownBy(() -> remoteGitService.pushExactBranch(
                        Path.of(otherTask.repositoryPath()),
                        bareRemote.toUri().toString(),
                        gitOperation.branchName(),
                        otherTask.commitHash(),
                        null))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isIn("PR_PUSH_FAILED", "PR_REMOTE_COMMIT_MISMATCH");
    }
}
