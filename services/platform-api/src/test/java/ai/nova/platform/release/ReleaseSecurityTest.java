package ai.nova.platform.release;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.release.support.ReleaseTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = "nova.release.enabled=true")
@AutoConfigureMockMvc
class ReleaseSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtService jwtService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void createRequiresReleaseRun() throws Exception {
        String token = jwtService.createAccessToken(new AuthenticatedUser(
                ReleaseTestFixture.USER_ID,
                ReleaseTestFixture.ORG_ID,
                "viewer@nova.local",
                "Viewer",
                List.of("USER"),
                List.of("RELEASE_READ"),
                true));
        mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReleaseTestFixture.createBody("sec", "0.0.9", UUID.randomUUID(), "sec-sha")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void listRequiresReleaseRead() throws Exception {
        String token = jwtService.createAccessToken(new AuthenticatedUser(
                ReleaseTestFixture.USER_ID,
                ReleaseTestFixture.ORG_ID,
                "runner@nova.local",
                "Runner",
                List.of("USER"),
                List.of("RELEASE_RUN"),
                true));
        mockMvc.perform(get("/api/releases").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
