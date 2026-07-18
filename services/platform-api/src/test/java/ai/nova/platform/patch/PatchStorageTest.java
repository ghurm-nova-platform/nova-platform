package ai.nova.platform.patch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactResponse;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.patch.dto.PatchDtos.ParsedPatchOutput;
import ai.nova.platform.patch.dto.PatchDtos.PatchResult;
import ai.nova.platform.patch.entity.PatchStatus;
import ai.nova.platform.patch.repository.GeneratedPatchRepository;
import ai.nova.platform.patch.repository.PatchResultRepository;
import ai.nova.platform.patch.service.PatchDiffParser;
import ai.nova.platform.patch.service.PatchStorageService;

@SpringBootTest
@AutoConfigureMockMvc
class PatchStorageTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String VALID_PATCH = PatchValidationTest.VALID_PATCH;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private PatchStorageService patchStorageService;

    @Autowired
    private PatchDiffParser patchDiffParser;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private PatchResultRepository resultRepository;

    @Autowired
    private GeneratedPatchRepository generatedPatchRepository;

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
    void replaceResultIsLatestOnly() throws Exception {
        UUID taskId = createTaskWithArtifact("patch-store-" + UUID.randomUUID());
        var task = taskRepository.findById(taskId).orElseThrow();
        List<GeneratedArtifactResponse> artifacts =
                artifactStorageService.listByTask(taskId, task.getOrganizationId());

        ParsedPatchOutput first = new ParsedPatchOutput(
                "first", 1, 1, 0, VALID_PATCH, PatchStatus.VALID);
        patchStorageService.replaceResult(
                task, artifacts, first, patchDiffParser.parseAndValidate(VALID_PATCH), 5L, "m", "LOCAL", 3L);

        String secondPatch = """
                --- a/src/LoginService.java
                +++ b/src/LoginService.java
                @@ -1,1 +1,3 @@
                 class LoginService {}
                +// validated
                +// again
                """;
        ParsedPatchOutput second = new ParsedPatchOutput(
                "second", null, null, null, secondPatch, PatchStatus.VALID);
        PatchResult stored = patchStorageService.replaceResult(
                task,
                artifacts,
                second,
                patchDiffParser.parseAndValidate(secondPatch),
                8L,
                "patch-local",
                "LOCAL",
                4L);

        assertThat(stored.summary()).isEqualTo("second");
        assertThat(stored.statistics().insertions()).isEqualTo(2);
        assertThat(resultRepository.findAll().stream().filter((r) -> r.getTaskId().equals(taskId)).count())
                .isEqualTo(1);
        assertThat(generatedPatchRepository.findByPatchResultIdOrderByPathAsc(stored.id())).hasSize(1);
        assertThat(patchStorageService.findLatest(taskId, task.getOrganizationId()).summary()).isEqualTo("second");
    }

    private UUID createTaskWithArtifact(String name) throws Exception {
        UUID taskId = createTask(name);
        artifactStorageService.replaceArtifacts(
                taskRepository.findById(taskId).orElseThrow(),
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
                                  "modelReference":"patch-local",
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
}
