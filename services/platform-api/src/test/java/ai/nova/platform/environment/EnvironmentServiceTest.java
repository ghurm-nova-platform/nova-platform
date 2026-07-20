package ai.nova.platform.environment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
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
import ai.nova.platform.environment.entity.EnvironmentStatus;
import ai.nova.platform.environment.support.EnvironmentTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = "nova.environment.enabled=true")
@AutoConfigureMockMvc
class EnvironmentServiceTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

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
        accessToken = jwtService.createAccessToken(EnvironmentTestFixture.environmentAdminUser());
    }

    @Test
    void createIdempotentEnableDisableArchive() throws Exception {
        String name = "staging-" + UUID.randomUUID();
        String body = EnvironmentTestFixture.createBody(name, "STAGING");

        MvcResult created = mockMvc.perform(post("/api/environments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EnvironmentStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.environmentType").value("STAGING"))
                .andReturn();
        String environmentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/environments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(environmentId));

        mockMvc.perform(post("/api/environments/" + environmentId + "/disable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EnvironmentStatus.DISABLED.name()))
                .andExpect(jsonPath("$.active").value(false));

        mockMvc.perform(post("/api/environments/" + environmentId + "/enable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EnvironmentStatus.ACTIVE.name()))
                .andExpect(jsonPath("$.active").value(true));

        mockMvc.perform(put("/api/environments/" + environmentId)
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EnvironmentTestFixture.updateBody("updated description")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("updated description"))
                .andExpect(jsonPath("$.region").value("eu-west-1"));

        mockMvc.perform(post("/api/environments/" + environmentId + "/archive")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(EnvironmentStatus.ARCHIVED.name()));

        mockMvc.perform(get("/api/environments/" + environmentId + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    void listByProject() throws Exception {
        String name = "list-" + UUID.randomUUID();
        mockMvc.perform(post("/api/environments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EnvironmentTestFixture.createBody(name, "QA")))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/environments")
                        .param("projectId", EnvironmentTestFixture.PROJECT_ID.toString())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.name == '" + name + "')]").exists());
    }

    @Test
    void testAliasMapsToTesting() throws Exception {
        String name = "test-alias-" + UUID.randomUUID();
        mockMvc.perform(post("/api/environments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EnvironmentTestFixture.createBody(name, "TEST")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environmentType").value("TESTING"));
    }

    @Test
    void duplicateProductionRejected() throws Exception {
        String first = "prod-a-" + UUID.randomUUID();
        String second = "prod-b-" + UUID.randomUUID();
        mockMvc.perform(post("/api/environments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EnvironmentTestFixture.createBody(first, "PRODUCTION")))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/environments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EnvironmentTestFixture.createBody(second, "PRODUCTION")))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ENVIRONMENT_DUPLICATE_TYPE"));
    }
}
