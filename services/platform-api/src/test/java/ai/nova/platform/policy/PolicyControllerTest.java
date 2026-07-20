package ai.nova.platform.policy;

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
import org.springframework.test.web.servlet.MvcResult;

import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.policy.entity.PolicyDecision;
import ai.nova.platform.policy.support.PolicyTestFixture;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = {"nova.policy.enabled=true", "nova.release.enabled=true"})
@AutoConfigureMockMvc
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ReleaseStorageService releaseStorageService;

    @Autowired
    private ReleaseManifestService releaseManifestService;

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
        accessToken = jwtService.createAccessToken(PolicyTestFixture.policyAdminUser());
    }

    @Test
    void listGetAndHistory() throws Exception {
        String name = "ctrl-" + UUID.randomUUID();
        MvcResult created = mockMvc.perform(post("/api/policies")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.createBody(name, "MANIFEST_INTEGRITY", "ALL_REQUIRED", 15, "{}")))
                .andExpect(status().isOk())
                .andReturn();
        String policyId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        UUID releaseId = seedPublished("82.6." + PolicyTestFixture.uniquePatch());

        mockMvc.perform(post("/api/policies/" + policyId + "/evaluate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.evaluateBody(releaseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestEvaluation.decision").value(PolicyDecision.PASSED.name()));

        mockMvc.perform(get("/api/policies")
                        .header("Authorization", "Bearer " + accessToken)
                        .param("projectId", PolicyTestFixture.PROJECT_ID.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());

        mockMvc.perform(get("/api/policies/" + policyId).header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(policyId))
                .andExpect(jsonPath("$.policyType").value("MANIFEST_INTEGRITY"));

        mockMvc.perform(get("/api/policies/" + policyId + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline").isArray())
                .andExpect(jsonPath("$.latestEvaluation.evidence").isArray());
    }

    @Test
    void notFoundReturnsCode() throws Exception {
        mockMvc.perform(get("/api/policies/" + UUID.randomUUID())
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POLICY_NOT_FOUND"));
    }

    private UUID seedPublished(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                "Ctrl Rel " + version,
                "controller notes",
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 1, 0, 0, VersionBump.PATCH),
                "ctrl-fp-" + version + "-" + mergeId,
                PolicyTestFixture.USER_ID,
                List.of(new ContentSpec(ReleaseContentType.MERGE_OPERATION, mergeId, null)),
                List.of());
        releaseStorageService.markPreparing(draft.getId());
        var manifest = releaseManifestService.build(
                releaseStorageService.require(draft.getId()),
                releaseStorageService.contents(draft.getId()),
                releaseStorageService.artifacts(draft.getId()));
        releaseStorageService.markReady(draft.getId(), manifest);
        releaseStorageService.markPublished(draft.getId());
        return draft.getId();
    }
}
