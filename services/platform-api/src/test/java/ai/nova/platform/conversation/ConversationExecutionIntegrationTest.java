package ai.nova.platform.conversation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

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

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.ExecutionResult;
import ai.nova.platform.conversation.entity.ConversationMessageRole;
import ai.nova.platform.conversation.repository.ConversationExecutionRequestRepository;
import ai.nova.platform.conversation.repository.ConversationMessageRepository;

@SpringBootTest
@AutoConfigureMockMvc
class ConversationExecutionIntegrationTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ConversationMessageRepository conversationMessageRepository;

    @Autowired
    private ConversationExecutionRequestRepository executionRequestRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void loginAndStubRuntime() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        doAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            return new ExecutionResult(
                    "assistant reply for " + request.executionId(), 5, 10, 15, 42L);
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
    void executeWithConversationAddsUserAndAssistantMessages() throws Exception {
        String conversationId = createConversation();
        UUID clientRequestId = UUID.randomUUID();

        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(executeBody(conversationId, clientRequestId, "Hello conversation")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.response").isNotEmpty());

        var messages = conversationMessageRepository.findAllByConversationIdAndOrganizationIdAndProjectIdOrderBySequenceNumberAsc(
                UUID.fromString(conversationId),
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                UUID.fromString(PROJECT_ID));
        assertThat(messages).hasSize(2);
        assertThat(messages.get(0).getRole()).isEqualTo(ConversationMessageRole.USER);
        assertThat(messages.get(0).getContent()).isEqualTo("Hello conversation");
        assertThat(messages.get(1).getRole()).isEqualTo(ConversationMessageRole.ASSISTANT);
    }

    @Test
    void failedExecutionAddsUserOnly() throws Exception {
        doAnswer(invocation -> {
            throw new RuntimeException("runtime boom");
        }).when(agentRuntimeClient).execute(any(ExecutionRequest.class));

        String conversationId = createConversation();
        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(executeBody(conversationId, UUID.randomUUID(), "fail me")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"));

        assertMessageCount(conversationId, 1, ConversationMessageRole.USER);
    }

    @Test
    void cancelledExecutionAddsUserOnly() throws Exception {
        CountDownLatch runtimeStarted = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        AtomicReference<UUID> executionIdRef = new AtomicReference<>();

        doAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            executionIdRef.set(request.executionId());
            runtimeStarted.countDown();
            if (!allowFinish.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("timeout");
            }
            return new ExecutionResult("should-not-persist", 1, 1, 2, 10L);
        }).when(agentRuntimeClient).execute(any(ExecutionRequest.class));

        String conversationId = createConversation();
        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<MvcResult> executeFuture = pool.submit(() -> mockMvc.perform(
                            post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                                    .header("Authorization", "Bearer " + accessToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(executeBody(conversationId, UUID.randomUUID(), "slow")))
                    .andReturn());

            assertThat(runtimeStarted.await(5, TimeUnit.SECONDS)).isTrue();
            UUID executionId = executionIdRef.get();
            mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/executions/" + executionId + "/cancel")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk());

            allowFinish.countDown();
            MvcResult result = executeFuture.get(5, TimeUnit.SECONDS);
            assertThat(objectMapper.readTree(result.getResponse().getContentAsString()).get("status").asText())
                    .isEqualTo("CANCELLED");
            assertMessageCount(conversationId, 1, ConversationMessageRole.USER);
        } finally {
            allowFinish.countDown();
            pool.shutdownNow();
        }
    }

    @Test
    void duplicateClientRequestIdDoesNotDuplicateMessagesOrExecutions() throws Exception {
        String conversationId = createConversation();
        UUID clientRequestId = UUID.randomUUID();

        MvcResult first = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(executeBody(conversationId, clientRequestId, "once")))
                .andExpect(status().isOk())
                .andReturn();
        String firstExecutionId = objectMapper
                .readTree(first.getResponse().getContentAsString())
                .get("executionId")
                .asText();

        MvcResult second = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(executeBody(conversationId, clientRequestId, "once")))
                .andExpect(status().isOk())
                .andReturn();
        String secondExecutionId = objectMapper
                .readTree(second.getResponse().getContentAsString())
                .get("executionId")
                .asText();

        assertThat(secondExecutionId).isEqualTo(firstExecutionId);
        assertThat(executionRequestRepository.findAll().stream()
                        .filter(r -> r.getConversationId().equals(UUID.fromString(conversationId)))
                        .count())
                .isEqualTo(1);
        assertMessageCount(conversationId, 2, null);
    }

    @Test
    void statelessExecutionUnchanged() throws Exception {
        mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"stateless hello"},
                                  "variables":{"customer_name":"Alex","topic":"billing"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.renderedPrompt").isNotEmpty());
    }

    private String createConversation() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/conversations")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"agentId":"%s","title":"Exec integration"}
                                """.formatted(DEMO_AGENT_ID)))
                .andExpect(status().isCreated())
                .andReturn();
        return objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText();
    }

    private static String executeBody(String conversationId, UUID clientRequestId, String message) {
        return """
                {
                  "input":{"message":"%s"},
                  "variables":{"customer_name":"Alex","topic":"billing"},
                  "conversationId":"%s",
                  "clientRequestId":"%s"
                }
                """.formatted(message, conversationId, clientRequestId);
    }

    private void assertMessageCount(String conversationId, int expectedCount, ConversationMessageRole onlyRole)
            throws Exception {
        var messages = conversationMessageRepository
                .findAllByConversationIdAndOrganizationIdAndProjectIdOrderBySequenceNumberAsc(
                        UUID.fromString(conversationId),
                        UUID.fromString("11111111-1111-1111-1111-111111111111"),
                        UUID.fromString(PROJECT_ID));
        assertThat(messages).hasSize(expectedCount);
        if (onlyRole != null) {
            assertThat(messages).allMatch(m -> m.getRole() == onlyRole);
        }
    }
}
