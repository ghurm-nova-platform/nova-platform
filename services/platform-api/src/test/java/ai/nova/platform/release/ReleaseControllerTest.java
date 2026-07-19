package ai.nova.platform.release;

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

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.release.support.ReleaseTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = "nova.release.enabled=true")
@AutoConfigureMockMvc
class ReleaseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    private String accessToken;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        accessToken = jwtService.createAccessToken(ReleaseTestFixture.releaseAdminUser());
    }

    @Test
    void createEndpointReturnsDraftRelease() throws Exception {
        UUID mergeId = UUID.randomUUID();
        mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReleaseTestFixture.createBody(
                                "Ctrl-" + mergeId, "5.1.0", mergeId, "ctrl-" + mergeId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.releaseName").value("Ctrl-" + mergeId))
                .andExpect(jsonPath("$.releaseNumber").isNumber())
                .andExpect(jsonPath("$.status").value("DRAFT"));
    }

    @Test
    void getRequiresExistingRelease() throws Exception {
        mockMvc.perform(get("/api/releases/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound());
    }
}
