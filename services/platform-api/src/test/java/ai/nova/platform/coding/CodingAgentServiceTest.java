package ai.nova.platform.coding;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.coding.dto.CodingDtos.CodeGenerationRequest;
import ai.nova.platform.coding.dto.CodingDtos.CodingResult;
import ai.nova.platform.coding.repository.GeneratedArtifactRepository;
import ai.nova.platform.coding.service.CodingAgentService;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest
@AutoConfigureMockMvc
class CodingAgentServiceTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final UUID ORG_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID USER_ID = UUID.fromString("44444444-4444-4444-4444-444444444401");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private CodingAgentService codingAgentService;

    @Autowired
    private GeneratedArtifactRepository artifactRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;
    private AuthenticatedUser user;

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
        user = new AuthenticatedUser(
                USER_ID,
                ORG_ID,
                "admin@nova.local",
                "Nova Admin",
                List.of("ORG_ADMIN"),
                List.of("CODING_GENERATE", "CODING_READ", "ORCHESTRATION_RUN_CREATE", "ORCHESTRATION_TASK_MANAGE"),
                true);
    }

    @Test
    void generateStoresArtifactsAndInvokesModelOutsideTransaction() throws Exception {
        UUID taskId = createTask("coding-svc-" + UUID.randomUUID());
        AtomicBoolean sawOpenTx = new AtomicBoolean(false);
        when(agentRuntimeClient.execute(any(ExecutionRequest.class))).thenAnswer(invocation -> {
            sawOpenTx.set(TransactionSynchronizationManager.isActualTransactionActive());
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse(
                    """
                    {
                      "summary":"Implemented login page",
                      "artifacts":[
                        {
                          "type":"SOURCE_FILE",
                          "language":"JAVA",
                          "path":"src/main/java/LoginService.java",
                          "filename":"LoginService.java",
                          "content":"class LoginService {}"
                        },
                        {
                          "type":"TEST",
                          "language":"JAVA",
                          "path":"src/test/java/LoginServiceTest.java",
                          "filename":"LoginServiceTest.java",
                          "content":"class LoginServiceTest {}"
                        }
                      ]
                    }
                    """,
                    10,
                    20,
                    30,
                    5L));
        });

        CodingResult result = codingAgentService.generate(new CodeGenerationRequest(taskId), user);

        assertThat(result.validated()).isTrue();
        assertThat(result.summary()).isEqualTo("Implemented login page");
        assertThat(result.artifacts()).hasSize(2);
        assertThat(artifactRepository.findByTaskIdAndOrganizationIdOrderByPathAsc(taskId, ORG_ID)).hasSize(2);
        assertThat(sawOpenTx.get()).isFalse();

        ArgumentCaptor<ExecutionRequest> captor = ArgumentCaptor.forClass(ExecutionRequest.class);
        verify(agentRuntimeClient).execute(captor.capture());
        assertThat(captor.getValue().systemPrompt()).contains("Coding Agent");
        assertThat(captor.getValue().messages().get(0).content()).contains("Build login");
    }

    @Test
    void generateRejectsInvalidModelJson() throws Exception {
        UUID taskId = createTask("coding-bad-" + UUID.randomUUID());
        when(agentRuntimeClient.execute(any(ExecutionRequest.class)))
                .thenReturn(RuntimeTurnResult.finalResponse(
                        new RuntimeFinalResponse("{\"summary\":\"x\",\"artifacts\":[]}", 1, 1, 2, 1L)));

        assertThatThrownBy(() -> codingAgentService.generate(new CodeGenerationRequest(taskId), user))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("CODING_EMPTY_ARTIFACTS");
        assertThat(artifactRepository.findByTaskIdAndOrganizationIdOrderByPathAsc(taskId, ORG_ID)).isEmpty();
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
                                  "description":"agentRole=coding; Create login service",
                                  "taskType":"AGENT_TURN",
                                  "modelReference":"coding-local",
                                  "inputJson":"{\\"feature\\":\\"login\\"}",
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
