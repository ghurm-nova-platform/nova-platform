package ai.nova.platform.repair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.repository.PatchResultRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.repair.config.RepairProperties;
import ai.nova.platform.repair.dto.RepairDtos.RepairOperation;
import ai.nova.platform.repair.dto.RepairDtos.RepairRunRequest;
import ai.nova.platform.repair.repository.RepairOperationRepository;
import ai.nova.platform.repair.service.RepairAgentService;
import ai.nova.platform.repair.service.RepairFingerprint;
import ai.nova.platform.repair.service.RepairFingerprint.InputLine;
import ai.nova.platform.repair.service.RepairInputCollector;
import ai.nova.platform.review.service.ReviewStorageService;
import ai.nova.platform.repair.support.RepairTestFixture;

@SpringBootTest
@AutoConfigureMockMvc
class RepairIdempotencyTest {

    private static final String VALID_PATCH = RepairTestFixture.VALID_PATCH;
    private static final String REPAIR_PATCH = """
            --- a/src/LoginService.java
            +++ b/src/LoginService.java
            @@ -1,1 +1,2 @@
             class LoginService {}
            +// repair fix
            """;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RepairAgentService repairAgentService;

    @Autowired
    private RepairProperties repairProperties;

    @Autowired
    private RepairInputCollector inputCollector;

    @Autowired
    private RepairOperationRepository operationRepository;

    @Autowired
    private PatchResultRepository patchResultRepository;

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
    private ObjectMapper objectMapper;

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
        when(agentRuntimeClient.execute(any(ExecutionRequest.class))).thenAnswer(invocation -> {
            String body = objectMapper.writeValueAsString(java.util.Map.of(
                    "summary", "Repair patch",
                    "reason", "Fix",
                    "confidence", 0.8,
                    "filesChanged", 1,
                    "insertions", 1,
                    "deletions", 0,
                    "repairedFiles", List.of("src/LoginService.java"),
                    "patch", REPAIR_PATCH,
                    "status", "VALID"));
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(body, 5, 10, 15, 3L));
        });

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
    void fingerprintIsStableForSameInputs() throws Exception {
        UUID taskId = createTask("repair-fp-" + UUID.randomUUID());
        var task = taskRepository.findById(taskId).orElseThrow();
        RepairTestFixture.seedArtifact(task, artifactStorageService);
        var prior = RepairTestFixture.seedPriorPatch(
                task, artifactStorageService, patchStorageService, patchDiffParser, VALID_PATCH);
        RepairTestFixture.seedFailedReview(task, artifactStorageService, reviewStorageService);

        var inputs = inputCollector.collect(task, prior);
        String fp1 = RepairFingerprint.compute(
                inputs.stream().map(i -> new InputLine(i.sourceType(), i.detail())).toList(), prior.id());
        String fp2 = RepairFingerprint.compute(
                inputs.stream().map(i -> new InputLine(i.sourceType(), i.detail())).toList(), prior.id());
        assertThat(fp1).isEqualTo(fp2);
        assertThat(fp1).hasSize(64);
    }

    @Test
    void duplicateRunReturnsSameSucceededOperation() throws Exception {
        UUID taskId = createRepairReadyTask("repair-idem-run-" + UUID.randomUUID());

        RepairOperation first = repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser());
        RepairOperation second = repairAgentService.run(new RepairRunRequest(taskId), RepairTestFixture.adminUser());
        var task = taskRepository.findById(taskId).orElseThrow();

        assertThat(second.id()).isEqualTo(first.id());
        assertThat(operationRepository.countByTaskIdAndOrganizationId(taskId, task.getOrganizationId()))
                .isEqualTo(1);
        assertThat(patchResultRepository.countByTaskIdAndOrganizationId(taskId, task.getOrganizationId()))
                .isEqualTo(2);
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
                                  "taskKey":"repair-idem",
                                  "displayName":"Repair idem",
                                  "description":"agentRole=repair",
                                  "taskType":"AGENT_TURN",
                                  "modelReference":"repair-local",
                                  "maxAttempts":1,
                                  "retryBackoffMs":0,
                                  "priority":1,
                                  "timeoutSeconds":60,
                                  "idempotencyKey":"repair-idem"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode task = objectMapper.readTree(taskResult.getResponse().getContentAsString());
        return UUID.fromString(task.get("id").asText());
    }
}
