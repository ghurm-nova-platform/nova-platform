package ai.nova.platform.orchestration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.orchestration.entity.RunStatus;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.repository.AgentOrchestrationRunRepository;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.service.OrchestrationExecutionService;
import ai.nova.platform.orchestration.service.TaskClaimService;

@SpringBootTest
@AutoConfigureMockMvc
class OrchestrationRunControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";
    private static final String OTHER_ORG_TOKEN_EMAIL = "should-not-exist@example.com";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskClaimService claimService;

    @Autowired
    private OrchestrationExecutionService executionService;

    @Autowired
    private AgentOrchestrationRunRepository runRepository;

    @Autowired
    private AgentOrchestrationTaskRepository taskRepository;

    private String accessToken;

    @BeforeEach
    void login() throws Exception {
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
    void createReadyStartCancelSequentialAgentTurn() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"seq-run",
                                  "objective":"Run one agent turn",
                                  "executionMode":"SEQUENTIAL",
                                  "failurePolicy":"FAIL_FAST",
                                  "maxParallelTasks":1,
                                  "maximumDurationMs":3600000
                                }
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"))
                .andReturn();

        String runId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskKey":"turn-1",
                                  "displayName":"Agent Turn",
                                  "taskType":"AGENT_TURN",
                                  "assignedAgentId":"%s",
                                  "maxAttempts":1,
                                  "retryBackoffMs":1000,
                                  "priority":100,
                                  "timeoutSeconds":60,
                                  "sequenceOrder":1,
                                  "inputJson":"{\\"message\\":\\"hello orchestration\\"}"
                                }
                                """.formatted(DEMO_AGENT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("DRAFT"));

        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/ready")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("READY"));

        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/start")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"));

        var claimed = claimService.claimReadyTasks(10);
        assertThat(claimed).isNotEmpty();
        for (var task : claimed) {
            executionService.executeClaimedTask(task.getId());
        }

        mockMvc.perform(get("/api/orchestration-runs/" + runId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.taskStatusCounts.SUCCEEDED").value(1));

        MvcResult cancelCreate = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"cancel-run",
                                  "objective":"Cancel me",
                                  "executionMode":"SEQUENTIAL",
                                  "failurePolicy":"FAIL_FAST",
                                  "maxParallelTasks":1,
                                  "maximumDurationMs":3600000
                                }
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String cancelRunId =
                objectMapper.readTree(cancelCreate.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/orchestration-runs/" + cancelRunId + "/cancel")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"test cancel\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void organizationIsolation() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"iso-run",
                                  "objective":"isolation",
                                  "executionMode":"SEQUENTIAL",
                                  "failurePolicy":"FAIL_FAST",
                                  "maxParallelTasks":1,
                                  "maximumDurationMs":3600000
                                }
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID runId = UUID.fromString(
                objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText());

        assertThat(runRepository.findByIdAndOrganizationId(runId, UUID.randomUUID())).isEmpty();
        assertThat(taskRepository.findByRunIdAndOrganizationId(runId, UUID.randomUUID())).isEmpty();

        mockMvc.perform(get("/api/orchestration-runs/" + runId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RunStatus.DRAFT.name()));
    }

    @Test
    void emptyRunCannotBecomeReady() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"empty-run",
                                  "objective":"no tasks",
                                  "executionMode":"SEQUENTIAL",
                                  "failurePolicy":"FAIL_FAST",
                                  "maxParallelTasks":1,
                                  "maximumDurationMs":3600000
                                }
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String runId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/ready")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("ORCHESTRATION_EMPTY"));
    }
}
