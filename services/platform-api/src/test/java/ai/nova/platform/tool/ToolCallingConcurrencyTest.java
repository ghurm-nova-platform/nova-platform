package ai.nova.platform.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeToolCallBatch;
import ai.nova.platform.agent.runtime.RuntimeToolCallRequest;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.tool.repository.ExecutionToolCallRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ToolCallingConcurrencyTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";
    private static final String RUNTIME_CALL_ID = "dup-runtime-call-1";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExecutionToolCallRepository toolCallRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void loginAndStubRuntime() throws Exception {
        AtomicInteger runtimeCalls = new AtomicInteger(0);
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        doAnswer(invocation -> {
            int callNumber = runtimeCalls.incrementAndGet();
            ObjectNode arguments = objectMapper.createObjectNode();
            arguments.put("operation", "ADD");
            arguments.put("left", 1);
            arguments.put("right", 2);
            RuntimeToolCallRequest toolCall =
                    new RuntimeToolCallRequest(RUNTIME_CALL_ID, "CALCULATOR", arguments);
            if (callNumber <= 2) {
                return RuntimeTurnResult.toolCalls(new RuntimeToolCallBatch(List.of(toolCall)));
            }
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse("done", 1, 1, 2, 5L));
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
    void duplicateRuntimeCallIdCreatesSingleExecutionRecord() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"duplicate runtime call"},
                                  "variables":{"customer_name":"Alex","topic":"billing"}
                                }
                                """))
                .andExpect(status().isOk())
                .andReturn();

        UUID executionId = UUID.fromString(objectMapper
                .readTree(result.getResponse().getContentAsString())
                .get("executionId")
                .asText());

        long matchingCalls = toolCallRepository.findAll().stream()
                .filter(call -> RUNTIME_CALL_ID.equals(call.getRuntimeCallId())
                        && call.getExecutionId().equals(executionId))
                .count();
        assertThat(matchingCalls).isEqualTo(1);
    }
}
