package ai.nova.platform.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
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
import ai.nova.platform.approval.service.ApprovalStorageService;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.git.service.GitStorageService;
import ai.nova.platform.merge.dto.MergeDtos.MergeOperation;
import ai.nova.platform.merge.entity.MergeMethod;
import ai.nova.platform.merge.entity.MergeStatus;
import ai.nova.platform.merge.entity.MergeValidationResult;
import ai.nova.platform.merge.service.MergeStorageService;
import ai.nova.platform.merge.service.MergeValidator.ValidationCheck;
import ai.nova.platform.merge.support.MergeTestFixture;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.pullrequest.repository.ProjectRepositoryConfigRepository;
import ai.nova.platform.pullrequest.service.ProjectRepositoryConfigService;
import ai.nova.platform.pullrequest.service.PullRequestStorageService;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.testing.service.TestingStorageService;

@SpringBootTest
@AutoConfigureMockMvc
class MergeStorageTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MergeStorageService storageService;

    @Autowired
    private ApprovalStorageService approvalStorageService;

    @Autowired
    private PatchStorageService patchStorageService;

    @Autowired
    private GitStorageService gitStorageService;

    @Autowired
    private PullRequestStorageService pullRequestStorageService;

    @Autowired
    private CiStorageService ciStorageService;

    @Autowired
    private ApprovalPolicyRepository policyRepository;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private GitIntegrationAgentService gitAgentService;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private PatchDiffParser patchDiffParser;

    @Autowired
    private ReviewStorageService reviewStorageService;

    @Autowired
    private TestingStorageService testingStorageService;

    @Autowired
    private ProjectRepositoryConfigService repositoryConfigService;

    @Autowired
    private ControlledGitService controlledGitService;

    @Autowired
    private GitProperties gitProperties;

    @Autowired
    private ProjectRepositoryConfigRepository configRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private UUID taskId;
    private AuthenticatedUser user;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        String accessToken = MergeTestFixture.loginAdmin(mockMvc, objectMapper);
        user = MergeTestFixture.mergeAdminUser();
        taskId = MergeTestFixture.seedApprovedTask(
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
                Path.of(System.getProperty("java.io.tmpdir"), "nova-merge-storage-tests"),
                "merge-storage-");
    }

    @Test
    void persistAndReloadMergeOperation() {
        var task = taskRepository.findById(taskId).orElseThrow();
        var approval = approvalStorageService.findLatest(taskId, MergeTestFixture.ORG_ID);
        var patch = patchStorageService.findLatest(taskId, MergeTestFixture.ORG_ID);
        var git = gitStorageService.findLatest(taskId, MergeTestFixture.ORG_ID);
        var pr = pullRequestStorageService.findLatest(taskId, MergeTestFixture.ORG_ID);
        var ci = ciStorageService.findLatest(taskId, MergeTestFixture.ORG_ID);

        UUID operationId = UUID.randomUUID();
        Instant now = Instant.now();
        storageService.startPending(
                operationId, task, approval, patch, git, pr, ci, MergeMethod.SQUASH, git.commitHash(), now);
        storageService.saveValidations(
                operationId,
                List.of(new ValidationCheck(
                        "APPROVAL_APPROVED",
                        "APPROVED",
                        "APPROVED",
                        MergeValidationResult.PASSED,
                        null,
                        true)),
                now);
        storageService.updateStatus(operationId, MergeStatus.VALIDATING);
        MergeOperation succeeded = storageService.markSucceeded(
                operationId,
                "MOCK",
                "merged-sha",
                pr.pullRequestUrl(),
                MergeTestFixture.USER_ID,
                "ok",
                false,
                true);

        assertThat(succeeded.status()).isEqualTo(MergeStatus.SUCCEEDED);
        assertThat(succeeded.result()).isNotNull();
        assertThat(succeeded.result().mergedCommit()).isEqualTo("merged-sha");
        assertThat(succeeded.validations()).hasSize(1);

        MergeOperation latest = storageService.findLatest(taskId, MergeTestFixture.ORG_ID, true);
        assertThat(latest.id()).isEqualTo(operationId);
    }
}
