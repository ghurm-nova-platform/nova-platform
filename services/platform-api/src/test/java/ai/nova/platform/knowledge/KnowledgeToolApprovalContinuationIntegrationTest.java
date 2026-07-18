package ai.nova.platform.knowledge;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeKnowledgeContext;
import ai.nova.platform.agent.runtime.RuntimeToolCallBatch;
import ai.nova.platform.agent.runtime.RuntimeToolCallRequest;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.tool.entity.ToolCallStatus;

@SpringBootTest
@AutoConfigureMockMvc
class KnowledgeToolApprovalContinuationIntegrationTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String AGENT_ID = "66666666-6666-6666-6666-666666666601";
    private static final String DEMO_KB_ID = "88888888-8888-8888-8888-888888888801";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;
    private final AtomicReference<RuntimeKnowledgeContext> firstTurnContext = new AtomicReference<>();
    private final AtomicReference<RuntimeKnowledgeContext> continuationContext = new AtomicReference<>();

    @BeforeEach
    void loginAndStubRuntime() throws Exception {
        firstTurnContext.set(null);
        continuationContext.set(null);
        AtomicBoolean firstTurn = new AtomicBoolean(true);

        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        doAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            if (firstTurn.getAndSet(false)
                    && (request.toolResults() == null || request.toolResults().isEmpty())) {
                firstTurnContext.set(request.knowledgeContext());
                ObjectNode arguments = objectMapper.createObjectNode();
                arguments.put("timezone", "Asia/Riyadh");
                RuntimeToolCallRequest toolCall = new RuntimeToolCallRequest(
                        "knowledge-approval-1", "APPROVAL_KB_DATETIME", arguments);
                return RuntimeTurnResult.toolCalls(new RuntimeToolCallBatch(List.of(toolCall)));
            }
            continuationContext.set(request.knowledgeContext());
            int citationCount = request.knowledgeContext() == null
                    ? 0
                    : request.knowledgeContext().citations().size();
            String label = request.knowledgeContext() == null
                            || request.knowledgeContext().citations().isEmpty()
                    ? "none"
                    : request.knowledgeContext().citations().getFirst().label();
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(
                    "knowledgeCitations=" + citationCount + " first=" + label, 3, 5, 8, 20L));
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
    void approvalContinuationPreservesOriginalKnowledgeContextAndCitations() throws Exception {
        String content = "Refund policy for Nova: refunds are issued within five business days.";
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "refunds.txt",
                "text/plain",
                content.getBytes(StandardCharsets.UTF_8));
        mockMvc.perform(multipart(
                                "/api/projects/" + PROJECT_ID + "/knowledge-bases/" + DEMO_KB_ID + "/documents")
                        .file(file)
                        .param("documentKey", "REF_" + UUID.randomUUID().toString().substring(0, 6).toUpperCase())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("READY"));

        String toolKey = "APPROVAL_KB_DATETIME";
        MvcResult createResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/tools")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "toolKey":"%s",
                                  "name":"Approval KB datetime",
                                  "description":"Requires approval with RAG",
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
        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + AGENT_ID + "/tools")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"toolId":"%s"}
                                """.formatted(toolId)))
                .andExpect(status().isCreated());

        MvcResult executeResult = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"What is the refund policy timeline?"},
                                  "variables":{"customer_name":"Ada","topic":"refunds"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("RUNNING"))
                .andExpect(jsonPath("$.awaitingApproval").value(true))
                .andExpect(jsonPath("$.citations").isArray())
                .andReturn();

        JsonNode executeBody = objectMapper.readTree(executeResult.getResponse().getContentAsString());
        UUID executionId = UUID.fromString(executeBody.get("executionId").asText());
        UUID pendingToolCallId = UUID.fromString(executeBody.get("pendingToolCallId").asText());
        JsonNode originalCitations = executeBody.get("citations");
        assertThat(originalCitations.size()).isGreaterThan(0);
        assertThat(firstTurnContext.get()).isNotNull();
        assertThat(firstTurnContext.get().isEmpty()).isFalse();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/executions/" + executionId + "/tool-calls/"
                                + pendingToolCallId + "/approve")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"version":0}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ToolCallStatus.APPROVED.name()));

        MvcResult continueResult = mockMvc.perform(post(
                                "/api/projects/" + PROJECT_ID + "/executions/" + executionId + "/continue")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.citations").isArray())
                .andReturn();

        JsonNode continueBody = objectMapper.readTree(continueResult.getResponse().getContentAsString());
        JsonNode continuedCitations = continueBody.get("citations");
        assertThat(continuedCitations.size()).isEqualTo(originalCitations.size());
        assertThat(continuedCitations.get(0).get("label").asText())
                .isEqualTo(originalCitations.get(0).get("label").asText());
        assertThat(continueBody.get("response").asText())
                .contains("knowledgeCitations=" + originalCitations.size());

        assertThat(continuationContext.get()).isNotNull();
        assertThat(continuationContext.get().isEmpty()).isFalse();
        assertThat(continuationContext.get().citations()).hasSize(firstTurnContext.get().citations().size());
        assertThat(continuationContext.get().citations().getFirst().label())
                .isEqualTo(firstTurnContext.get().citations().getFirst().label());
        assertThat(continuationContext.get().chunks().getFirst().content())
                .isEqualTo(firstTurnContext.get().chunks().getFirst().content());
    }
}
