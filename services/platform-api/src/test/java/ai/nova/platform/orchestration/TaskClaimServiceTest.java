package ai.nova.platform.orchestration;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import ai.nova.platform.orchestration.entity.AgentOrchestrationTask;
import ai.nova.platform.orchestration.entity.TaskStatus;
import ai.nova.platform.orchestration.repository.AgentOrchestrationTaskRepository;
import ai.nova.platform.orchestration.service.TaskClaimService;

@SpringBootTest
@AutoConfigureMockMvc
class TaskClaimServiceTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private TaskClaimService claimService;

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
    void doubleClaimIsAtomic() throws Exception {
        MvcResult create = mockMvc.perform(post("/api/orchestration-runs")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "name":"claim-run",
                                  "objective":"claim race",
                                  "executionMode":"SEQUENTIAL",
                                  "failurePolicy":"FAIL_FAST",
                                  "maxParallelTasks":1,
                                  "maximumDurationMs":3600000
                                }
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        String runId = objectMapper.readTree(create.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/tasks")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "taskKey":"claim-task",
                                  "displayName":"Claim Task",
                                  "taskType":"AGENT_TURN",
                                  "assignedAgentId":"%s",
                                  "maxAttempts":1,
                                  "retryBackoffMs":1000,
                                  "priority":10,
                                  "timeoutSeconds":60,
                                  "sequenceOrder":1
                                }
                                """.formatted(DEMO_AGENT_ID)))
                .andExpect(status().isCreated());

        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/ready")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/orchestration-runs/" + runId + "/start")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        var first = claimService.claimReadyTasks(10);
        var second = claimService.claimReadyTasks(10);

        assertThat(first).hasSize(1);
        assertThat(second).isEmpty();

        AgentOrchestrationTask task = taskRepository.findById(first.get(0).getId()).orElseThrow();
        assertThat(task.getStatus()).isEqualTo(TaskStatus.CLAIMED);
        assertThat(task.getClaimedBy()).isNotBlank();
    }
}
