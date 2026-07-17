package ai.nova.platform.tool;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

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
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeToolCallBatch;
import ai.nova.platform.agent.runtime.RuntimeToolCallRequest;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.tool.entity.ToolCallStatus;

@SpringBootTest
@AutoConfigureMockMvc
class ToolApprovalIntegrationTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void loginAndStubRuntime() throws Exception {
        AtomicBoolean firstTurn = new AtomicBoolean(true);
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        doAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            if (firstTurn.getAndSet(false) && (request.toolResults() == null || request.toolResults().isEmpty())) {
                ObjectNode arguments = objectMapper.createObjectNode();
                arguments.put("timezone", "Asia/Riyadh");
                RuntimeToolCallRequest toolCall = new RuntimeToolCallRequest(
                        "approval-call-1", "APPROVAL_TEST_DATETIME", arguments);
                return RuntimeTurnResult.toolCalls(new RuntimeToolCallBatch(List.of(toolCall)));
            }
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(
                    "Approved tool completed for " + request.executionId(), 3, 5, 8, 20L));
        }).when(agentRuntimeClient).execute(any(ExecutionRequest.class));

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
    void approvalRequiredToolCanBeApprovedAndContinued() throws Exception {
        String toolKey = "APPROVAL_TEST_DATETIME";
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/tools")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolKey":"%s",
                                  "name":"Approval test datetime",
                                  "description":"Requires approval",
                                  "executorKey":"CURRENT_DATETIME",
                                  "inputSchema":"{\\"type\\":\\"object\\",\\"properties\\":{\\"timezone\\":{\\"type\\":\\"string\\",\\"maxLength\\":100}},\\"required\\":[\\"timezone\\"],\\"additionalProperties\\":false}",
                                  "requiresApproval":true,
                                  "maxExecutionSeconds":5,
                                  "maxOutputCharacters":5000
                                }
                                """.formatted(toolKey)))
                .andExpect(status().isCreated())
                .andReturn();

        String toolId = objectMapper.readTree(createResult.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/tools/" + toolId + "/activate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/tools")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolId":"%s"}
                                """.formatted(toolId)))
                .andExpect(status().isCreated());

        MvcResult executeResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"Need approved datetime"},
                                  "variables":{"customer_name":"Alex","topic":"billing"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.awaitingApproval").value(true))
                .andExpect(jsonPath("$.pendingToolCallId").isNotEmpty())
                .andReturn();

        JsonNode executeBody = objectMapper.readTree(executeResult.getResponse().getContentAsString());
        UUID executionId = UUID.fromString(executeBody.get("executionId").asText());
        UUID pendingToolCallId = UUID.fromString(executeBody.get("pendingToolCallId").asText());

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/executions/" + executionId + "/tool-calls/"
                                + pendingToolCallId + "/approve")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ToolCallStatus.APPROVED.name()));

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/executions/" + executionId + "/continue")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.response").isNotEmpty());
    }
}
