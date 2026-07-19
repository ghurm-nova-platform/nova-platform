package ai.nova.platform.pullrequest.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

import org.eclipse.jgit.api.Git;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.dto.GitDtos.GitOperation;
import ai.nova.platform.git.dto.GitDtos.GitRunRequest;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.git.support.TestGitSourceFixture;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.security.AuthenticatedUser;

/**
 * Shared Pull Request Agent test fixture: local bare remotes, project remote rebinding,
 * and Git Integration Agent happy-path setup.
 */
public final class PullRequestTestFixture {

    public static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    public static final UUID PROJECT_UUID = UUID.fromString(PROJECT_ID);
    public static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    public static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    public static final String VALID_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class LoginService {}
            +// validated
            """;
    /** Valid unified diff that fails JGit apply due to context mismatch. */
    public static final String BAD_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class WrongContent {}
            +// fail
            """;

    private PullRequestTestFixture() {
    }

    public static AuthenticatedUser adminUser() {
        return new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("GIT_RUN", "GIT_READ", "PR_RUN", "PR_READ"),
                true);
    }

    public static void ensureSourceRepository(
            ControlledGitService controlledGitService, GitProperties gitProperties) throws Exception {
        TestGitSourceFixture.ensureSourceRepository(
                controlledGitService,
                ORG_ID,
                PROJECT_UUID,
                gitProperties.getBaseRef(),
                TestGitSourceFixture.loginServiceSeeds());
    }

    public static Path ensureBareRemote(Path parentDir) throws Exception {
        Files.createDirectories(parentDir);
        Path barePath = parentDir.resolve("remote-" + UUID.randomUUID() + ".git");
        Git.init().setDirectory(barePath.toFile()).setBare(true).call();
        return barePath.toAbsolutePath().normalize();
    }

    public static void pointProjectRemoteTo(
            ProjectRepositoryConfigRepository configRepository,
            UUID organizationId,
            UUID projectId,
            Path bareRemote) {
        var config = configRepository
                .findByOrganizationIdAndProjectId(organizationId, projectId)
                .orElseThrow();
        config.setRemoteUrl(bareRemote.toUri().toString());
        config.setRepositoryHost("localhost");
        configRepository.save(config);
    }

    public static GitOperation createSuccessfulGitOperationViaAgent(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String accessToken,
            AuthenticatedUser user,
            GitIntegrationAgentService gitAgentService,
            ArtifactStorageService artifactStorageService,
            PatchStorageService patchStorageService,
            PatchDiffParser patchDiffParser,
            AgentOrchestrationTaskRepository taskRepository,
            String taskNamePrefix) throws Exception {
        UUID taskId = createTaskWithApprovedPatch(
                mockMvc,
                objectMapper,
                accessToken,
                taskNamePrefix + UUID.randomUUID(),
                artifactStorageService,
                patchStorageService,
                patchDiffParser,
                taskRepository);
        return gitAgentService.run(new GitRunRequest(taskId), user);
    }

    public static UUID createTaskWithApprovedPatch(
            MockMvc mockMvc,
            ObjectMapper objectMapper,
            String accessToken,
            String name,
            ArtifactStorageService artifactStorageService,
            PatchStorageService patchStorageService,
            PatchDiffParser patchDiffParser,
            AgentOrchestrationTaskRepository taskRepository) throws Exception {
        UUID taskId = createTask(mockMvc, objectMapper, accessToken, name);
        AgentOrchestrationTask task = taskRepository.findById(taskId).orElseThrow();
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
                new ParsedPatchOutput("Generated patch", 1, 1, 0, VALID_PATCH, PatchStatus.VALID);
        patchStorageService.replaceResult(
                task,
                artifacts,
                parsed,
                patchDiffParser.parseAndValidate(VALID_PATCH),
                5L,
                "patch-local",
                "LOCAL",
                3L);
        return taskId;
    }

    public static UUID createTask(MockMvc mockMvc, ObjectMapper objectMapper, String accessToken, String name)
            throws Exception {
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

    public static String loginAdmin(MockMvc mockMvc, ObjectMapper objectMapper) throws Exception {
        MvcResult loginResult = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"admin@nova.local","password":"ChangeMe123!"}
                                """))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(loginResult.getResponse().getContentAsString())
                .get("accessToken")
                .asText();
    }
}
