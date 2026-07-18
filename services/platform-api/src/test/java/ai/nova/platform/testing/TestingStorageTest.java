package ai.nova.platform.testing;

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
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.testing.dto.TestingDtos.GeneratedTestDraft;
import ai.nova.platform.testing.dto.TestingDtos.ParsedTestingOutput;
import ai.nova.platform.testing.dto.TestingDtos.TestingResult;
import ai.nova.platform.testing.entity.TestPriority;
import ai.nova.platform.testing.entity.TestType;
import ai.nova.platform.testing.repository.GeneratedTestRepository;
import ai.nova.platform.testing.repository.TestingResultRepository;
import ai.nova.platform.testing.service.TestingStorageService;

@SpringBootTest
@AutoConfigureMockMvc
class TestingStorageTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArtifactStorageService artifactStorageService;

    @Autowired
    private TestingStorageService testingStorageService;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    @Autowired
    private TestingResultRepository resultRepository;

    @Autowired
    private GeneratedTestRepository testRepository;

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
    void persistsCoverageSummaryTestsCasesAndArtifactRefs() throws Exception {
        UUID taskId = createTask("testing-store-" + UUID.randomUUID());
        var task = taskRepository.findById(taskId).orElseThrow();
        var artifacts = artifactStorageService.replaceArtifacts(
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

        TestingResult stored = testingStorageService.replaceResult(
                task,
                artifacts,
                new ParsedTestingOutput(
                        "Unit and API tests generated.",
                        84,
                        List.of(new GeneratedTestDraft(
                                TestType.UNIT,
                                TestPriority.HIGH,
                                "LoginService validation",
                                "Verify invalid credentials.",
                                "src/LoginService.java",
                                List.of()))),
                30L,
                "testing-local",
                "LOCAL",
                12L);

        assertThat(stored.coverageEstimate()).isEqualTo(84);
        assertThat(stored.generatedTests()).hasSize(1);
        assertThat(stored.testCases()).hasSize(1);
        assertThat(stored.reviewedArtifacts()).hasSize(1);
        assertThat(testRepository.findByTestingResultIdOrderByPriorityDescTitleAsc(stored.id())).hasSize(1);
        assertThat(resultRepository.findFirstByTaskIdAndOrganizationIdOrderByCreatedAtDesc(
                        taskId, task.getOrganizationId()))
                .isPresent();
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
                                  "modelReference":"testing-local",
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
