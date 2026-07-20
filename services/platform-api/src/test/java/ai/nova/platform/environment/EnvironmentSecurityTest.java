package ai.nova.platform.environment;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.environment.support.EnvironmentTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = "nova.environment.enabled=true")
@AutoConfigureMockMvc
class EnvironmentSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void readWithoutPermissionForbidden() throws Exception {
        String token = jwtService.createAccessToken(EnvironmentTestFixture.environmentRunOnlyUser());
        mockMvc.perform(get("/api/environments")
                        .param("projectId", EnvironmentTestFixture.PROJECT_ID.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void createWithoutRunForbidden() throws Exception {
        String token = jwtService.createAccessToken(EnvironmentTestFixture.environmentReadOnlyUser());
        mockMvc.perform(post("/api/environments")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EnvironmentTestFixture.createBody("sec-test", "QA")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void readWithReadPermissionAllowed() throws Exception {
        String token = jwtService.createAccessToken(EnvironmentTestFixture.environmentReadOnlyUser());
        mockMvc.perform(get("/api/environments")
                        .param("projectId", EnvironmentTestFixture.PROJECT_ID.toString())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }
}
