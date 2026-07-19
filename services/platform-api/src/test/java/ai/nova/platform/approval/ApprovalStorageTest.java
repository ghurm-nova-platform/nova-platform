package ai.nova.platform.approval;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.nio.file.Path;
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
import ai.nova.platform.approval.dto.ApprovalDtos.ApprovalDecision;
import ai.nova.platform.approval.entity.ApprovalDecisionValue;
import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.approval.service.ApprovalStorageService;
import ai.nova.platform.approval.support.ApprovalTestFixture;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.service.TestingStorageService;

@SpringBootTest
@AutoConfigureMockMvc
class ApprovalStorageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ApprovalStorageService storageService;

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

    private AuthenticatedUser user;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        user = ApprovalTestFixture.adminUser();
    }

    @Test
    void findLatestReturnsPersistedDecision() throws Exception {
        String token = PullRequestTestFixture.loginAdmin(mockMvc, objectMapper);
        UUID taskId = ApprovalTestFixture.seedTaskWithFullPipeline(
                mockMvc,
                objectMapper,
                token,
                user,
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
                Path.of(System.getProperty("java.io.tmpdir"), "nova-approval-storage-tests"),
                "approval-store-");

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/approval-gate/run")
                        .header("Authorization", "Bearer " + token)
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());

        ApprovalDecision latest = storageService.findLatest(taskId, ApprovalTestFixture.ORG_ID);
        assertThat(latest).isNotNull();
        assertThat(latest.taskId()).isEqualTo(taskId);
        assertThat(latest.requirements()).isNotEmpty();
        assertThat(latest.evidence()).isNotEmpty();
    }
}
