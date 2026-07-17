package ai.nova.platform.tool;

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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.NoOpAgentRuntimeClient;
import ai.nova.platform.execution.entity.MessageRole;
import ai.nova.platform.execution.repository.ExecutionMessageRepository;
import ai.nova.platform.tool.entity.ToolCallStatus;
import ai.nova.platform.tool.repository.ExecutionToolCallRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ToolCallingExecutionIntegrationTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ExecutionToolCallRepository toolCallRepository;

    @Autowired
    private ExecutionMessageRepository executionMessageRepository;

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
    void calculatorToolCallCompletesWithAssistantAndToolMessages() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"%s"},
                                  "variables":{"customer_name":"Alex","topic":"billing"}
                                }
                                """.formatted(NoOpAgentRuntimeClient.MARKER_CALCULATOR)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.response").isNotEmpty())
                .andReturn();

        JsonNode body = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID executionId = UUID.fromString(body.get("executionId").asText());

        var toolCalls = toolCallRepository.findByExecutionIdAndProjectIdAndOrganizationIdOrderBySequenceNumberAsc(
                executionId,
                UUID.fromString(PROJECT_ID),
                UUID.fromString("11111111-1111-1111-1111-111111111111"));
        org.assertj.core.api.Assertions.assertThat(toolCalls).hasSize(1);
        org.assertj.core.api.Assertions.assertThat(toolCalls.getFirst().getStatus())
                .isEqualTo(ToolCallStatus.COMPLETED);
        org.assertj.core.api.Assertions.assertThat(toolCalls.getFirst().getToolKey()).isEqualTo("CALCULATOR");

        var executionMessages = executionMessageRepository.findByExecutionIdOrderByCreatedAtAsc(executionId);
        org.assertj.core.api.Assertions.assertThat(executionMessages.stream().map(m -> m.getRole()).toList())
                .contains(MessageRole.TOOL, MessageRole.ASSISTANT);
    }
}
