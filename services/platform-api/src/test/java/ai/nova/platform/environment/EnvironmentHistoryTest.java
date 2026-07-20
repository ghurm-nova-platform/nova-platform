package ai.nova.platform.environment;

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
import ai.nova.platform.environment.support.EnvironmentTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = "nova.environment.enabled=true")
@AutoConfigureMockMvc
class EnvironmentHistoryTest {

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
    void historyContainsSnapshotsAndTimeline() throws Exception {
        String name = "history-" + UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/environments")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(EnvironmentTestFixture.createBody(name, "DEVELOPMENT")))
                .andExpect(status().isOk())
                .andReturn();
        String environmentId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/environments/" + environmentId + "/disable")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        String historyJson = mockMvc.perform(get("/api/environments/" + environmentId + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.history").isArray())
                .andExpect(jsonPath("$.timeline").isArray())
                .andReturn()
                .getResponse()
                .getContentAsString();

        assertThat(historyJson).contains("CREATED", "STATUS_CHANGED", "DISABLED");
    }
}
