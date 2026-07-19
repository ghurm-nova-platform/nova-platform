package ai.nova.platform.ci;

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
import ai.nova.platform.ci.support.CiTestFixture;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@SpringBootTest
@AutoConfigureMockMvc
class CiSecurityTest {

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
    void rejectsRunWithoutCiRunPermission() throws Exception {
        String limitedToken = jwtService.createAccessToken(new AuthenticatedUser(
                PullRequestTestFixture.USER_ID,
                PullRequestTestFixture.ORG_ID,
                "viewer@nova.local",
                "Viewer",
                List.of("USER"),
                List.of("CI_READ"),
                true));

        mockMvc.perform(post("/api/ci/run")
                        .header("Authorization", "Bearer " + limitedToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"taskId\":\"" + UUID.randomUUID() + "\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }

    @Test
    void rejectsGetLatestWithoutCiReadPermission() throws Exception {
        String limitedToken = jwtService.createAccessToken(new AuthenticatedUser(
                PullRequestTestFixture.USER_ID,
                PullRequestTestFixture.ORG_ID,
                "runner@nova.local",
                "Runner",
                List.of("USER"),
                List.of("CI_RUN"),
                true));

        mockMvc.perform(get("/api/ci/" + UUID.randomUUID()).header("Authorization", "Bearer " + limitedToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("FORBIDDEN"));
    }
}
