package ai.nova.platform.merge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.approval.repository.ApprovalPolicyRepository;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.ci.service.CiStorageService;
import ai.nova.platform.git.config.GitProperties;
import ai.nova.platform.git.service.ControlledGitService;
import ai.nova.platform.git.service.GitIntegrationAgentService;
import ai.nova.platform.merge.entity.MergeEventEntity;
import ai.nova.platform.merge.entity.MergeEventType;
import ai.nova.platform.merge.entity.MergeStatus;
import ai.nova.platform.merge.provider.MergeProvider;
import ai.nova.platform.merge.provider.MergeProvider.MergeOutcome;
import ai.nova.platform.merge.provider.MergeProvider.MergeRequest;
import ai.nova.platform.merge.provider.MergeProvider.RemotePullRequestState;
import ai.nova.platform.merge.repository.MergeEventRepository;
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
import ai.nova.platform.web.error.ApiException;

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

    @Autowired
    private MergeEventRepository mergeEventRepository;

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
        MergeTestFixture.stubSuccessfulMergeProvider(mergeProvider, "merged-sha-123");
        accessToken = MergeTestFixture.loginAdmin(mockMvc, objectMapper);
        user = MergeTestFixture.mergeAdminUser();
    }

    @Test
    void happyPathMergeSucceedsAfterRemoteVerification() throws Exception {
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
    void verifyPassedEmittedOnlyAfterSuccessfulRemoteVerification() throws Exception {
        UUID taskId = seedApproved("merge-verify-passed-");
        MvcResult result = mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.SUCCEEDED.name()))
                .andReturn();
        UUID operationId = UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
        List<MergeEventType> types = mergeEventRepository.findByMergeOperationIdOrderByCreatedAtAsc(operationId).stream()
                .map(MergeEventEntity::getEventType)
                .toList();
        assertThat(types).contains(MergeEventType.VERIFY_STARTED, MergeEventType.VERIFY_PASSED);
        assertThat(types.indexOf(MergeEventType.VERIFY_STARTED))
                .isLessThan(types.indexOf(MergeEventType.VERIFY_PASSED));
        assertThat(types).doesNotContain(MergeEventType.VERIFY_FAILED);

        // Failure path must never emit VERIFY_PASSED.
        UUID failTaskId = seedApproved("merge-verify-no-pass-");
        AtomicReference<Boolean> afterMerge = new AtomicReference<>(false);
        doAnswer(invocation -> {
            MergeRequest request = invocation.getArgument(0, MergeRequest.class);
            afterMerge.set(true);
            return MergeOutcome.success(
                    request.headSha(), "merged-sha-open", Instant.now(), "bot", request.pullRequestUrl(), "merged");
        }).when(mergeProvider).merge(any(), anyString());
        doAnswer(invocation -> {
            long number = invocation.getArgument(1, Long.class);
            return new RemotePullRequestState(
                    number,
                    "https://github.com/ghurm-nova-platform/nova-demo/pull/" + number,
                    "PR",
                    "feature",
                    "main",
                    "open",
                    false,
                    null,
                    null,
                    null,
                    null,
                    "ghurm-nova-platform",
                    "nova-demo");
        }).when(mergeProvider).getPullRequest(any(), anyLong(), anyString());
        MvcResult failed = mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + failTaskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.FAILED.name()))
                .andReturn();
        UUID failedOpId = UUID.fromString(objectMapper.readTree(failed.getResponse().getContentAsString()).get("id").asText());
        List<MergeEventType> failedTypes = mergeEventRepository.findByMergeOperationIdOrderByCreatedAtAsc(failedOpId).stream()
                .map(MergeEventEntity::getEventType)
                .toList();
        assertThat(failedTypes).contains(MergeEventType.VERIFY_STARTED, MergeEventType.VERIFY_FAILED);
        assertThat(failedTypes).doesNotContain(MergeEventType.VERIFY_PASSED);
    }

    @Test
    void mergeResponseSucceedsButRemotePrRemainsOpenFails() throws Exception {
        UUID taskId = seedApproved("merge-still-open-");
        AtomicReference<String> head = new AtomicReference<>();
        doAnswer(invocation -> {
            MergeRequest request = invocation.getArgument(0, MergeRequest.class);
            head.set(request.headSha());
            return MergeOutcome.success(
                    request.headSha(), "merged-sha-open", Instant.now(), "bot", request.pullRequestUrl(), "merged");
        }).when(mergeProvider).merge(any(), anyString());
        doAnswer(invocation -> {
            long number = invocation.getArgument(1, Long.class);
            return new RemotePullRequestState(
                    number,
                    "https://github.com/ghurm-nova-platform/nova-demo/pull/" + number,
                    "PR",
                    "feature",
                    "main",
                    "open",
                    false,
                    null,
                    null,
                    null,
                    null,
                    "ghurm-nova-platform",
                    "nova-demo");
        }).when(mergeProvider).getPullRequest(any(), anyLong(), anyString());
        mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.FAILED.name()))
                .andExpect(jsonPath("$.errorCode").value("MERGE_REMOTE_STATE_MISMATCH"));
    }

    @Test
    void verificationLookupFailureDoesNotSucceed() throws Exception {
        UUID taskId = seedApproved("merge-verify-lookup-");
        AtomicReference<Boolean> afterMerge = new AtomicReference<>(false);
        doAnswer(invocation -> {
            MergeRequest request = invocation.getArgument(0, MergeRequest.class);
            afterMerge.set(true);
            return MergeOutcome.success(
                    request.headSha(), "merged-sha-x", Instant.now(), "bot", request.pullRequestUrl(), "merged");
        }).when(mergeProvider).merge(any(), anyString());
        doAnswer(invocation -> {
            long number = invocation.getArgument(1, Long.class);
            if (!afterMerge.get()) {
                return new RemotePullRequestState(
                        number,
                        "https://github.com/pr/" + number,
                        "PR",
                        "feature",
                        "main",
                        "open",
                        false,
                        null,
                        null,
                        null,
                        null,
                        "ghurm-nova-platform",
                        "nova-demo");
            }
            throw new ApiException(HttpStatus.BAD_GATEWAY, "MERGE_PROVIDER_FAILED", "lookup failed");
        }).when(mergeProvider).getPullRequest(any(), anyLong(), anyString());
        mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.FAILED.name()))
                .andExpect(jsonPath("$.errorCode").value("MERGE_VERIFICATION_FAILED"));
    }

    @Test
    void remoteHeadMismatchFails() throws Exception {
        UUID taskId = seedApproved("merge-head-mismatch-");
        AtomicReference<Boolean> afterMerge = new AtomicReference<>(false);
        doAnswer(invocation -> {
            MergeRequest request = invocation.getArgument(0, MergeRequest.class);
            afterMerge.set(true);
            return MergeOutcome.success(
                    request.headSha(), "merged-sha-hm", Instant.now(), "bot", request.pullRequestUrl(), "merged");
        }).when(mergeProvider).merge(any(), anyString());
        doAnswer(invocation -> {
            long number = invocation.getArgument(1, Long.class);
            if (!afterMerge.get()) {
                return new RemotePullRequestState(
                        number,
                        "https://github.com/pr/" + number,
                        "PR",
                        "feature",
                        "main",
                        "open",
                        false,
                        null,
                        null,
                        null,
                        null,
                        "ghurm-nova-platform",
                        "nova-demo");
            }
            return new RemotePullRequestState(
                    number,
                    "https://github.com/pr/" + number,
                    "PR",
                    "feature",
                    "main",
                    "closed",
                    true,
                    "totally-different-head",
                    "merged-sha-hm",
                    Instant.now(),
                    "bot",
                    "ghurm-nova-platform",
                    "nova-demo");
        }).when(mergeProvider).getPullRequest(any(), anyLong(), anyString());
        mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.FAILED.name()))
                .andExpect(jsonPath("$.errorCode").value("MERGE_REMOTE_HEAD_MISMATCH"));
    }

    @Test
    void ambiguousOutcomeResolvedByRemoteRefetchWithoutDuplicateMerge() throws Exception {
        UUID taskId = seedApproved("merge-ambiguous-");
        AtomicReference<String> head = new AtomicReference<>();
        AtomicInteger mergeCalls = new AtomicInteger();
        doAnswer(invocation -> {
            mergeCalls.incrementAndGet();
            MergeRequest request = invocation.getArgument(0, MergeRequest.class);
            head.set(request.headSha());
            return MergeOutcome.ambiguous(request.headSha(), request.pullRequestUrl(), "timeout");
        }).when(mergeProvider).merge(any(), anyString());
        doAnswer(invocation -> {
            long number = invocation.getArgument(1, Long.class);
            if (mergeCalls.get() == 0) {
                return new RemotePullRequestState(
                        number,
                        "https://github.com/pr/" + number,
                        "PR",
                        "feature",
                        "main",
                        "open",
                        false,
                        null,
                        null,
                        null,
                        null,
                        "ghurm-nova-platform",
                        "nova-demo");
            }
            return new RemotePullRequestState(
                    number,
                    "https://github.com/pr/" + number,
                    "PR",
                    "feature",
                    "main",
                    "closed",
                    true,
                    head.get(),
                    "resolved-merge-commit",
                    Instant.now(),
                    "bot",
                    "ghurm-nova-platform",
                    "nova-demo");
        }).when(mergeProvider).getPullRequest(any(), anyLong(), anyString());

        mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.SUCCEEDED.name()))
                .andExpect(jsonPath("$.result.mergedCommit").value("resolved-merge-commit"));

        // Retry must not call merge again — idempotent succeeded return.
        mockMvc.perform(post("/api/merge/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(MergeStatus.SUCCEEDED.name()))
                .andExpect(jsonPath("$.result.mergedCommit").value("resolved-merge-commit"));

        verify(mergeProvider, times(1)).merge(any(), anyString());
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
