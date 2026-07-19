package ai.nova.platform.repair;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;
import ai.nova.platform.repair.dto.RepairDtos.TimelineEvent;
import ai.nova.platform.repair.entity.RepairInputSource;
import ai.nova.platform.repair.entity.RepairStatus;
import ai.nova.platform.repair.repository.RepairOperationRepository;
import ai.nova.platform.repair.repository.RepairResultRepository;
import ai.nova.platform.repair.service.RepairInputCollector.CollectedInput;
import ai.nova.platform.repair.service.RepairStorageService;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.repair.support.RepairTestFixture;

@SpringBootTest
@AutoConfigureMockMvc
class RepairStorageTest {

    private static final String VALID_PATCH = RepairTestFixture.VALID_PATCH;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private RepairStorageService repairStorageService;

    @Autowired
    private RepairOperationRepository operationRepository;

    @Autowired
    private RepairResultRepository resultRepository;

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

    private String accessToken;

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
    }

    @Test
    void persistsOperationInputsAndResult() throws Exception {
        AgentOrchestrationTask task = taskRepository.findById(createTask("repair-store-" + UUID.randomUUID()))
                .orElseThrow();
        RepairTestFixture.seedArtifact(task, artifactStorageService);
        var priorPatch = RepairTestFixture.seedPriorPatch(
                task, artifactStorageService, patchStorageService, patchDiffParser, VALID_PATCH);

        UUID operationId = UUID.randomUUID();
        Instant started = Instant.now();
        List<TimelineEvent> timeline = List.of(new TimelineEvent("STARTED", started, "test"));

        repairStorageService.startPending(
                operationId,
                task,
                1,
                priorPatch.id(),
                "abc123fingerprint",
                "REVIEW: fix",
                started,
                timeline);
        repairStorageService.updateStatus(operationId, RepairStatus.COLLECTING, timeline);
        repairStorageService.saveInputs(
                operationId,
                List.of(new CollectedInput(RepairInputSource.REVIEW, "review-1", 5, "Missing validation")),
                timeline);

        var artifacts = artifactStorageService.listByTask(task.getId(), task.getOrganizationId());
        var repairedPatch = patchStorageService.appendResult(
                task,
                artifacts,
                new ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput(
                        "Repair patch", 1, 1, 0, VALID_PATCH, ai.nova.platform.patch.entity.PatchStatus.VALID),
                patchDiffParser.parseAndValidate(VALID_PATCH),
                5L,
                "repair-local",
                "LOCAL",
                3L);
        UUID newPatchId = repairedPatch.id();
        var stored = repairStorageService.markSucceeded(
                operationId,
                newPatchId,
                "Fixed review findings",
                0.85,
                "Address review",
                List.of("src/LoginService.java"),
                Instant.now(),
                timeline);

        assertThat(stored.status()).isEqualTo(RepairStatus.SUCCEEDED);
        assertThat(stored.newPatchResultId()).isEqualTo(newPatchId);
        assertThat(stored.inputs()).hasSize(1);
        assertThat(stored.repairedFiles()).contains("src/LoginService.java");
        assertThat(operationRepository.findById(operationId)).isPresent();
        assertThat(resultRepository.findByRepairOperationId(operationId)).isPresent();
        assertThat(repairStorageService.findLatest(task.getId(), task.getOrganizationId()).id())
                .isEqualTo(operationId);
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
                                  "taskKey":"repair-store",
                                  "displayName":"Repair store",
                                  "description":"agentRole=repair",
                                  "taskType":"AGENT_TURN",
                                  "modelReference":"repair-local",
                                  "maxAttempts":1,
                                  "retryBackoffMs":0,
                                  "priority":1,
                                  "timeoutSeconds":60,
                                  "idempotencyKey":"repair-store"
                                }
                                """))
                .andExpect(status().isCreated())
                .andReturn();
        JsonNode task = objectMapper.readTree(taskResult.getResponse().getContentAsString());
        return UUID.fromString(task.get("id").asText());
    }
}
