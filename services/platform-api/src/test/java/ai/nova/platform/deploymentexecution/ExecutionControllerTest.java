package ai.nova.platform.deploymentexecution;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deploymentexecution.entity.ExecutionStatus;
import ai.nova.platform.deploymentexecution.support.ExecutionSeedSupport;
import ai.nova.platform.deploymentexecution.support.ExecutionTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(
        properties = {
            "nova.execution.enabled=true",
            "nova.execution.provider=LOCAL",
            "nova.rollback.enabled=true",
            "nova.deployment.enabled=true",
            "nova.release.enabled=true",
            "nova.audit.enabled=true"
        })
@AutoConfigureMockMvc
class ExecutionControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ExecutionSeedSupport seedSupport;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        accessToken = jwtService.createAccessToken(ExecutionTestFixture.executionAdminUser());
    }

    @Test
    void createStartListHistoryAndLogs() throws Exception {
        var ctx = seedSupport.seedExecutionReadyContext(mockMvc, accessToken);
        String body = ExecutionTestFixture.createBody(
                ctx.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, "LOCAL");

        MvcResult created = mockMvc.perform(post("/api/deployment-executions/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ExecutionStatus.QUEUED.name()))
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/deployment-executions/" + id + "/start")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ExecutionStatus.COMPLETED.name()))
                .andExpect(jsonPath("$.result.success").value(true));

        mockMvc.perform(get("/api/deployment-executions")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("projectId", ExecutionTestFixture.PROJECT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/deployment-executions/" + id + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline").isArray());

        mockMvc.perform(get("/api/deployment-executions/" + id + "/logs")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    void idempotentCreateReturnsExisting() throws Exception {
        var ctx = seedSupport.seedExecutionReadyContext(mockMvc, accessToken);
        String body = ExecutionTestFixture.createBody(
                ctx.releaseId(), ExecutionTestFixture.STAGING_ENVIRONMENT_ID, "LOCAL");

        MvcResult first = mockMvc.perform(post("/api/deployment-executions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(post("/api/deployment-executions")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText())
                .isEqualTo(objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText());
    }

    @Test
    void notFoundReturnsCode() throws Exception {
        mockMvc.perform(get("/api/deployment-executions/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPLOYMENT_EXECUTION_NOT_FOUND"));
    }
}
