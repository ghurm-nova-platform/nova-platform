package ai.nova.platform.release;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.pullrequest.support.PullRequestTestFixture;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.support.ReleaseTestFixture;
import ai.nova.platform.security.AuthenticatedUser;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = "nova.release.enabled=true")
@AutoConfigureMockMvc
class ReleaseManagerServiceTest {

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
    void setUp() throws Exception {
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        AuthenticatedUser user = ReleaseTestFixture.releaseAdminUser();
        accessToken = jwtService.createAccessToken(user);
    }

    @Test
    void createPreparePublishLifecycle() throws Exception {
        UUID mergeId = UUID.randomUUID();
        String body = ReleaseTestFixture.createBody("Rel-A-" + mergeId, "1.0.0", mergeId, "commit-a-" + mergeId);

        MvcResult created = mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReleaseStatus.DRAFT.name()))
                .andExpect(jsonPath("$.semanticVersion").value("1.0.0"))
                .andExpect(jsonPath("$.contents").isArray())
                .andReturn();

        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(post("/api/releases/" + id + "/prepare")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReleaseStatus.READY.name()))
                .andExpect(jsonPath("$.manifestHash").isNotEmpty());

        mockMvc.perform(post("/api/releases/" + id + "/publish")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(ReleaseStatus.PUBLISHED.name()));

        mockMvc.perform(post("/api/releases/" + id + "/publish")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELEASE_ALREADY_PUBLISHED"));
    }

    @Test
    void identicalCreateIsIdempotent() throws Exception {
        UUID mergeId = UUID.randomUUID();
        String body = ReleaseTestFixture.createBody("Rel-Idem-" + mergeId, "2.0.0", mergeId, "commit-idem-" + mergeId);

        MvcResult first = mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        MvcResult second = mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode a = objectMapper.readTree(first.getResponse().getContentAsString());
        JsonNode b = objectMapper.readTree(second.getResponse().getContentAsString());
        org.assertj.core.api.Assertions.assertThat(a.get("id").asText()).isEqualTo(b.get("id").asText());
    }

    @Test
    void prepareTwiceWhenReadyFails() throws Exception {
        UUID mergeId = UUID.randomUUID();
        String body = ReleaseTestFixture.createBody("Rel-Ready-" + mergeId, "3.0.0", mergeId, "commit-ready-" + mergeId);
        MvcResult created = mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        mockMvc.perform(post("/api/releases/" + id + "/prepare")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/releases/" + id + "/prepare")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELEASE_ALREADY_READY"));
    }

    @Test
    void versionConflictOnDuplicateSemver() throws Exception {
        UUID merge1 = UUID.randomUUID();
        UUID merge2 = UUID.randomUUID();
        mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReleaseTestFixture.createBody("Rel-V1-" + merge1, "9.9.9", merge1, "c1-" + merge1)))
                .andExpect(status().isOk());
        mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReleaseTestFixture.createBody("Rel-V2-" + merge2, "9.9.9", merge2, "c2-" + merge2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("RELEASE_VERSION_CONFLICT"));
    }

    @Test
    void listAndHistory() throws Exception {
        UUID mergeId = UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/releases/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(ReleaseTestFixture.createBody("Rel-List-" + mergeId, "4.0.0", mergeId, "clist-" + mergeId)))
                .andExpect(status().isOk())
                .andReturn();
        String id = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();

        mockMvc.perform(get("/api/releases")
                        .param("projectId", PullRequestTestFixture.PROJECT_ID)
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/releases/" + id + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline").isArray())
                .andExpect(jsonPath("$.timeline[0].eventType").value("CREATED"));
    }
}
