package ai.nova.platform.testing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
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
import ai.nova.platform.coding.dto.CodingDtos.GeneratedArtifactDraft;
import ai.nova.platform.coding.entity.ArtifactLanguage;
import ai.nova.platform.coding.entity.ArtifactType;
import ai.nova.platform.coding.service.ArtifactStorageService;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;

@SpringBootTest
@AutoConfigureMockMvc
class TestingControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ArtifactStorageService artifactStorageService;

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
        when(agentRuntimeClient.execute(any(ExecutionRequest.class))).thenReturn(RuntimeTurnResult.finalResponse(
                new RuntimeFinalResponse(
                        """
                        {
                          "summary":"Unit and API tests generated.",
                          "coverageEstimate":84,
                          "generatedTests":[
                            {
                              "type":"UNIT",
                              "priority":"HIGH",
                              "title":"LoginService validation",
                              "description":"Verify invalid credentials."
                            }
                          ]
                        }
                        """,
                        5,
                        10,
                        15,
                        3L)));

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
    void runAndGetLatestTestingResult() throws Exception {
        UUID taskId = createTaskWithArtifact("testing-api-" + UUID.randomUUID());

        mockMvc.perform(post("/api/testing/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + taskId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.coverageEstimate").value(84))
                .andExpect(jsonPath("$.generatedTests.length()").value(1))
                .andExpect(jsonPath("$.priorityCounts.HIGH").value(1));

        MvcResult getResult = mockMvc.perform(get("/api/testing/" + taskId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.summary").value("Unit and API tests generated."))
                .andReturn();
        JsonNode body = objectMapper.readTree(getResult.getResponse().getContentAsString());
        assertThat(body.get("testCases").isArray()).isTrue();
        assertThat(body.get("testCases").size()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void runUnknownTaskReturnsNotFound() throws Exception {
        mockMvc.perform(post("/api/testing/run")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("TESTING_TASK_NOT_FOUND"));
    }

    private UUID createTaskWithArtifact(String name) throws Exception {
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
        UUID taskId = UUID.fromString(
                objectMapper.readTree(taskResult.getResponse().getContentAsString()).get("id").asText());
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
}
