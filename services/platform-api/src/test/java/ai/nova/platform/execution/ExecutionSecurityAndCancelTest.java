package ai.nova.platform.execution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.agent.runtime.ExecutionRequest;
import ai.nova.platform.agent.runtime.RuntimeFinalResponse;
import ai.nova.platform.agent.runtime.RuntimeTurnResult;
import ai.nova.platform.execution.entity.AgentExecution;
import ai.nova.platform.execution.entity.ExecutionMetric;
import ai.nova.platform.execution.entity.ExecutionStatus;
import ai.nova.platform.execution.repository.AgentExecutionRepository;
import ai.nova.platform.execution.repository.ExecutionMetricRepository;
import ai.nova.platform.execution.service.ExecutionLifecycleService;

@SpringBootTest
@AutoConfigureMockMvc
class ExecutionSecurityAndCancelTest {

    private static final String PROJECT_ID = "55555555-5555-5555-5555-555555555501";
    private static final String DEMO_AGENT_ID = "66666666-6666-6666-6666-666666666601";
    private static final String SECRET = "SECRET_PROVIDER_KEY=sk-leak-should-never-persist";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private AgentExecutionRepository executionRepository;

    @Autowired
    private ExecutionMetricRepository metricRepository;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void loginAndStubDefinitionSync() throws Exception {
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
    }

    @Test
    void runtimeFailurePersistsOnlySafeErrorMessage() throws Exception {
        doAnswer(invocation -> {
            throw new RuntimeException("Provider rejected request with " + SECRET);
        }).when(agentRuntimeClient).execute(any(ExecutionRequest.class));

        MvcResult result = mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                        .header("Authorization", "Bearer " + accessToken)
                        .header("X-Correlation-Id", "corr-safe-error-test")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "input":{"message":"trigger failure"},
                                  "variables":{"customer_name":"Alex","topic":"billing"}
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("FAILED"))
                .andExpect(jsonPath("$.errorMessage").value(ExecutionLifecycleService.SAFE_ERROR_MESSAGE))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        assertThat(body).doesNotContain(SECRET);
        assertThat(body).doesNotContain("sk-leak");

        UUID executionId = UUID.fromString(objectMapper.readTree(body).get("executionId").asText());
        AgentExecution stored = executionRepository.findById(executionId).orElseThrow();
        assertThat(stored.getStatus()).isEqualTo(ExecutionStatus.FAILED);
        assertThat(stored.getErrorMessage()).isEqualTo(ExecutionLifecycleService.SAFE_ERROR_MESSAGE);
        assertThat(stored.getErrorMessage()).doesNotContain(SECRET);

        List<ExecutionMetric> metrics = metricRepository.findByExecutionId(executionId);
        assertThat(metrics)
                .anyMatch(m -> "error_code".equals(m.getMetricName())
                        && ExecutionLifecycleService.ERROR_CODE_EXECUTION_FAILED.equals(m.getMetricValue()));
        assertThat(metrics)
                .anyMatch(m -> "correlation_id".equals(m.getMetricName())
                        && "corr-safe-error-test".equals(m.getMetricValue()));
        assertThat(metrics.stream().map(ExecutionMetric::getMetricValue).toList())
                .noneMatch(value -> value.contains(SECRET));

        mockMvc.perform(get("/api/projects/" + PROJECT_ID + "/executions/" + executionId)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.errorMessage").value(ExecutionLifecycleService.SAFE_ERROR_MESSAGE))
                .andExpect(jsonPath("$.errorMessage").value(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("sk-leak"))));
    }

    @Test
    void cancelDuringRuntimePreventsCompletedOverwrite() throws Exception {
        CountDownLatch runtimeStarted = new CountDownLatch(1);
        CountDownLatch allowFinish = new CountDownLatch(1);
        AtomicReference<UUID> executionIdRef = new AtomicReference<>();

        doAnswer(invocation -> {
            ExecutionRequest request = invocation.getArgument(0);
            executionIdRef.set(request.executionId());
            runtimeStarted.countDown();
            if (!allowFinish.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Timed out waiting to finish runtime");
            }
            return RuntimeTurnResult.finalResponse(new RuntimeFinalResponse("should-not-win", 1, 1, 2, 50L));
        }).when(agentRuntimeClient).execute(any(ExecutionRequest.class));

        ExecutorService pool = Executors.newSingleThreadExecutor();
        try {
            Future<MvcResult> executeFuture = pool.submit(() -> mockMvc.perform(
                            post("/api/projects/" + PROJECT_ID + "/agents/" + DEMO_AGENT_ID + "/execute")
                                    .header("Authorization", "Bearer " + accessToken)
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("""
                                            {
                                              "input":{"message":"slow run"},
                                              "variables":{"customer_name":"Alex","topic":"billing"}
                                            }
                                            """))
                    .andReturn());

            assertThat(runtimeStarted.await(5, TimeUnit.SECONDS)).isTrue();
            UUID executionId = executionIdRef.get();
            assertThat(executionId).isNotNull();

            mockMvc.perform(post("/api/projects/" + PROJECT_ID + "/executions/" + executionId + "/cancel")
                            .header("Authorization", "Bearer " + accessToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("CANCELLED"));

            allowFinish.countDown();
            MvcResult executeResult = executeFuture.get(5, TimeUnit.SECONDS);
            assertThat(executeResult.getResponse().getStatus()).isEqualTo(200);
            JsonNode executeBody = objectMapper.readTree(executeResult.getResponse().getContentAsString());
            assertThat(executeBody.get("status").asText()).isEqualTo("CANCELLED");
            assertThat(executeBody.get("response").isNull()).isTrue();

            AgentExecution stored = executionRepository.findById(executionId).orElseThrow();
            assertThat(stored.getStatus()).isEqualTo(ExecutionStatus.CANCELLED);
        } finally {
            allowFinish.countDown();
            pool.shutdownNow();
        }
    }
}
