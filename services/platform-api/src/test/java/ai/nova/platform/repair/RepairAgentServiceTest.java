package ai.nova.platform.repair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.repository.PatchResultRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.repair.config.RepairProperties;
import ai.nova.platform.repair.dto.RepairDtos.RepairOperation;
import ai.nova.platform.repair.dto.RepairDtos.RepairRunRequest;
import ai.nova.platform.repair.entity.RepairStatus;
import ai.nova.platform.repair.service.RepairAgentService;
import ai.nova.platform.repair.support.RepairTestFixture;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.review.dto.ReviewDtos.ParsedReviewOutput;
import ai.nova.platform.review.dto.ReviewDtos.ReviewFindingDraft;
import ai.nova.platform.review.entity.ReviewCategory;
import ai.nova.platform.review.entity.ReviewSeverity;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class RepairAgentServiceTest {

    private static final String VALID_PATCH = RepairTestFixture.VALID_PATCH;
    private static final String REPAIR_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,3 @@
             class LoginService {}
            +// repair fix
            +// validated
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepairAgentService repairAgentService;

    @Autowired
    private RepairProperties repairProperties;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private ReviewStorageService reviewStorageService;

    @Autowired
    private PatchStorageService patchStorageService;

    @Autowired
    private PatchDiffParser patchDiffParser;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private PatchResultRepository patchResultRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void setUp() throws Exception {
        repairProperties.setEnabled(true);
        repairProperties.setMaxAttempts(5);
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
    }

    @Test
    void runPersistsRepairPatchAndCallsModelOutsideTransaction() throws Exception {
        UUID taskId = createRepairReadyTask("repair-svc-" + UUID.randomUUID());
        var task = taskRepository.findById(taskId).orElseThrow();
        var priorPatch = patchStorageService.findLatest(taskId, task.getOrganizationId());

        AtomicBoolean sawOpenTx = new AtomicBoolean(false);
        when(agentRuntimeClient.execute(any(ExecutionRequest.class))).thenAnswer(invocation -> {
            sawOpenTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "summary", "Repair patch",
                    "reason", "Fix review findings",
                    "confidence", 0.88,
                    "filesChanged", 1,
                    "insertions", 2,
                    "deletions", 0,
                    "repairedFiles", List.of("src/LoginService.java"),
                    "patch", REPAIR_PATCH,
                    "status", "VALID"));
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(body, 8, 12, 20, 4L));
        });

        RepairOperation result = repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser());
        assertThat(result.status()).isEqualTo(RepairStatus.SUCCEEDED);
        assertThat(result.newPatchResultId()).isNotNull();
        assertThat(result.newPatchResultId()).isNotEqualTo(priorPatch.id());
        assertThat(result.priorPatchResultId()).isEqualTo(priorPatch.id());
        assertThat(result.inputs()).isNotEmpty();
        assertThat(sawOpenTx.get()).isFalse();

        assertThat(patchResultRepository.countByTaskIdAndOrganizationId(taskId, task.getOrganizationId()))
                .isEqualTo(2);

        ArgumentCaptor<ExecutionRequest> captor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(agentRuntimeClient).execute(captor.capture());
        assertThat(captor.getValue().systemPrompt()).contains("Repair Agent");
        assertThat(captor.getValue().messages().get(0).content()).contains("Missing validation");
    }

    @Test
    void runRejectsWhenDisabled() throws Exception {
        repairProperties.setEnabled(false);
        UUID taskId = createRepairReadyTask("repair-disabled-" + UUID.randomUUID());
        assertThatThrownBy(() -> repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REPAIR_DISABLED");
    }

    @Test
    void runRejectsWhenNoFailures() throws Exception {
        UUID taskId = createTask("repair-no-failures-" + UUID.randomUUID());
        var task = taskRepository.findById(taskId).orElseThrow();
        RepairTestFixture.seedArtifact(task, artifactStorageService);
        RepairTestFixture.seedPriorPatch(task, artifactStorageService, patchStorageService, patchDiffParser, VALID_PATCH);

        assertThatThrownBy(() -> repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REPAIR_INVALID_STATE");
    }

    @Test
    void runRejectsWhenNoPriorPatch() throws Exception {
        UUID taskId = createTask("repair-no-patch-" + UUID.randomUUID());
        var task = taskRepository.findById(taskId).orElseThrow();
        RepairTestFixture.seedArtifact(task, artifactStorageService);
        RepairTestFixture.seedFailedReview(task, artifactStorageService, reviewStorageService);

        assertThatThrownBy(() -> repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REPAIR_INPUT_NOT_FOUND");
    }

    @Test
    void runRejectsWhenLimitExceeded() throws Exception {
        repairProperties.setMaxAttempts(1);
        UUID taskId = createRepairReadyTask("repair-limit-" + UUID.randomUUID());
        when(agentRuntimeClient.execute(any(ExecutionRequest.class))).thenAnswer(invocation -> {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "summary", "Repair patch",
                    "reason", "Fix",
                    "confidence", 0.8,
                    "filesChanged", 1,
                    "insertions", 2,
                    "deletions", 0,
                    "repairedFiles", List.of("src/LoginService.java"),
                    "patch", REPAIR_PATCH,
                    "status", "VALID"));
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(body, 5, 10, 15, 3L));
        });
        repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser());

        var task = taskRepository.findById(taskId).orElseThrow();
        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(taskId, task.getOrganizationId());
        reviewStorageService.replaceReview(
                task,
                artifacts,
                new ParsedReviewOutput(
                        "Still failing",
                        35,
                        false,
                        List.of(new ReviewFindingDraft(
                                ReviewSeverity.HIGH,
                                ReviewCategory.SECURITY,
                                "Another issue",
                                "Different failure detail for limit test",
                                "Fix again",
                                "src/LoginService.java"))),
                2L,
                "review-local",
                "LOCAL",
                3L);

        assertThatThrownBy(() -> repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("REPAIR_LIMIT_EXCEEDED");
    }

    @Test
    void runReturnsExistingOnIdempotentFingerprint() throws Exception {
        UUID taskId = createRepairReadyTask("repair-idem-" + UUID.randomUUID());
        when(agentRuntimeClient.execute(any(ExecutionRequest.class))).thenAnswer(invocation -> {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "summary", "Repair patch",
                    "reason", "Fix",
                    "confidence", 0.8,
                    "filesChanged", 1,
                    "insertions", 2,
                    "deletions", 0,
                    "repairedFiles", List.of("src/LoginService.java"),
                    "patch", REPAIR_PATCH,
                    "status", "VALID"));
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(body, 5, 10, 15, 3L));
        });

        RepairOperation first = repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser());
        RepairOperation second = repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser());
        assertThat(second.id()).isEqualTo(first.id());
        assertThat(second.newPatchResultId()).isEqualTo(first.newPatchResultId());
    }

    private UUID createRepairReadyTask(String name) throws Exception {
        UUID taskId = createTask(name);
        var task = taskRepository.findById(taskId).orElseThrow();
        RepairTestFixture.seedArtifact(task, artifactStorageService);
        RepairTestFixture.seedPriorPatch(task, artifactStorageService, patchStorageService, patchDiffParser, VALID_PATCH);
        RepairTestFixture.seedFailedReview(task, artifactStorageService, reviewStorageService);
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
                                """.formatted(RepairTestFixture.PROJECT_ID, name)))
                .andExpect(status().isCreated())
                .andReturn();
        String runId = objectMapper.readTree(runResult.getResponse().getContentAsString()).get("id").asText();
        MvcResult taskResult = mockMvc.perform(post("/api/orchestration-runs/" + runId + "/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskKey":"repair-1",
                                  "displayName":"Repair login",
                                  "description":"agentRole=repair",
                                  "taskType":"AGENT_TURN",
                                  "modelReference":"repair-local",
                                  "maxAttempts":1,
                                  "retryBackoffMs":0,
                                  "priority":1,
                                  "timeoutSeconds":60,
                                  "idempotencyKey":"repair-1"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode task = objectMapper.readTree(taskResult.getResponse().getContentAsString());
        return UUID.fromString(task.get("id").asText());
    }
}
