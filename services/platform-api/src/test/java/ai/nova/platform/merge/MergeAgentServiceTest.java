package ai.nova.platform.merge;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.util.UUID;

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
import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.merge.entity.MergeStatus;
import ai.nova.platform.merge.provider.MergeProvider;
import ai.nova.platform.merge.provider.MergeProvider.MergeOutcome;
import ai.nova.platform.merge.support.MergeTestFixture;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.service.TestingStorageService;

@SpringBootTest(properties = "nova.merge.enabled=true")
@AutoConfigureMockMvc
class MergeAgentServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApprovalPolicyRepository policyRepository;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private GitIntegrationAgentService gitAgentService;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private PatchStorageService patchStorageService;

    @Autowired
    private PatchDiffParser patchDiffParser;

    @Autowired
    private ReviewStorageService reviewStorageService;

    @Autowired
    private TestingStorageService testingStorageService;

    @Autowired
    private PullRequestStorageService pullRequestStorageService;

    @Autowired
    private ProjectRepositoryConfigService repositoryConfigService;

    @Autowired
    private CiStorageService ciStorageService;

    @Autowired
    private ControlledGitService controlledGitService;

    @Autowired
    private GitProperties gitProperties;

    @Autowired
    private ProjectRepositoryConfigRepository configRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @MockitoBean
    private MergeProvider mergeProvider;

    private String accessToken;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        when(mergeProvider.providerId()).thenReturn("MOCK");
        when(mergeProvider.merge(any(), anyString()))
                .thenReturn(new MergeOutcome(true, false, "merged-sha-123", "https://github.com/pr/42", "merged"));
        accessToken = MergeTestFixture.loginAdmin(mockMvc, objectMapper);
        user = MergeTestFixture.mergeAdminUser();
    }

    @Test
    void happyPathMergeSucceeds() throws Exception {
        UUID taskId = seedApproved("merge-happy-");
        mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.SUCCEEDED.name()))
                .andExpect(jsonPath("$.result.mergedCommit").value("merged-sha-123"));
    }

    @Test
    void approvalNotApprovedFails() throws Exception {
        UUID taskId = MergeTestFixture.seedApprovedTask(
                mockMvc,
                objectMapper,
                accessToken,
                user,
                policyRepository,
                gitAgentService,
                artifactStorageService,
                patchStorageService,
                patchDiffParser,
                reviewStorageService,
                testingStorageService,
                pullRequestStorageService,
                repositoryConfigService,
                ciStorageService,
                controlledGitService,
                gitProperties,
                configRepository,
                taskRepository,
                Path.of(System.getProperty("java.io.tmpdir"), "nova-merge-not-approved"),
                "merge-not-approved-");
        mockMvc.perform(post("/api/approval-gate/" + taskId + "/reject")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"comment\":\"no\"}"))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("MERGE_APPROVAL_REQUIRED"));
    }

    @Test
    void patchMismatchFailsValidation() throws Exception {
        UUID taskId = seedApproved("merge-patch-mismatch-");
        var task = taskRepository.findById(taskId).orElseThrow();
        var artifacts = artifactStorageService.listByTask(taskId, MergeTestFixture.ORG_ID);
        String changedPatch = """
                --- a/src/LoginService.java
                +++ b/src/LoginService.java
                @@ -1,1 +1,3 @@
                 class LoginService {}
                +// changed
                +// again
                """;
        patchStorageService.appendResult(
                task,
                artifacts,
                new ParsedPatchOutput("changed patch", 1, 2, 0, changedPatch, PatchStatus.VALID),
                patchDiffParser.parseAndValidate(changedPatch),
                1L,
                "patch-local",
                "LOCAL",
                1L);
        mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.FAILED.name()))
                .andExpect(jsonPath("$.errorCode").value("MERGE_PATCH_MISMATCH"));
    }

    private UUID seedApproved(String prefix) throws Exception {
        return MergeTestFixture.seedApprovedTask(
                mockMvc,
                objectMapper,
                accessToken,
                user,
                policyRepository,
                gitAgentService,
                artifactStorageService,
                patchStorageService,
                patchDiffParser,
                reviewStorageService,
                testingStorageService,
                pullRequestStorageService,
                repositoryConfigService,
                ciStorageService,
                controlledGitService,
                gitProperties,
                configRepository,
                taskRepository,
                Path.of(System.getProperty("java.io.tmpdir"), "nova-merge-agent-tests"),
                prefix);
    }
}
