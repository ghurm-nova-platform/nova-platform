package ai.nova.platform.planner;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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

@SpringBootTest
@AutoConfigureMockMvc
class PlannerControllerTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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
                          "objective":"Build authentication",
                          "executionMode":"DEPENDENCY_GRAPH",
                          "failurePolicy":"FAIL_FAST",
                          "maxParallelTasks":2,
                          "estimatedComplexity":"MEDIUM",
                          "estimatedTokens":14500,
                          "estimatedDurationSeconds":180,
                          "tasks":[
                            {"taskKey":"analysis","displayName":"Analyze requirements","taskType":"AGENT_TURN","agentRole":"research","classification":"RESEARCH","priority":1},
                            {"taskKey":"implementation","displayName":"Implement feature","taskType":"AGENT_TURN","agentRole":"coding","classification":"CODING","priority":2},
                            {"taskKey":"review","displayName":"Review changes","taskType":"AGENT_TURN","agentRole":"review","classification":"REVIEW","priority":3}
                          ],
                          "dependencies":[
                            {"from":"analysis","to":"implementation","type":"SUCCESS"},
                            {"from":"implementation","to":"review","type":"SUCCESS"}
                          ]
                        }
                        """,
                        10,
                        20,
                        30,
                        5L)));

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
    void planReturnsValidatedExecutionPlan() throws Exception {
        mockMvc.perform(post("/api/planner/plan")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"projectId":"%s","objective":"Build authentication"}
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.validated").value(true))
                .andExpect(jsonPath("$.plan.tasks.length()").value(3))
                .andExpect(jsonPath("$.estimate.riskLevel").isNotEmpty())
                .andExpect(jsonPath("$.estimate.estimatedTokens").value(14500));
    }

    @Test
    void planAndCreateImportsDraftRun() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/planner/plan-and-create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "projectId":"%s",
                                  "objective":"Build authentication",
                                  "runName":"Auth plan draft"
                                }
                                """.formatted(PROJECT_ID)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.draftRun.status").value("DRAFT"))
                .andExpect(jsonPath("$.draftRun.taskStatusCounts.DRAFT").value(3))
                .andReturn();
        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        assertThat(body.path("draftRun").path("id").asText()).isNotBlank();
        assertThat(body.path("planner").path("plan").path("dependencies").size()).isEqualTo(2);
    }
}
