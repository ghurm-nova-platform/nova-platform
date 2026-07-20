package ai.nova.platform.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
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
class PolicyEvaluationTest {

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
    void releaseNotesRequiredFailsWithoutDescription() throws Exception {
        String policyId = createPolicy("notes-" + UUID.randomUUID(), "RELEASE_NOTES_REQUIRED", "{}");
        UUID releaseId = seedPublished("82.2." + PolicyTestFixture.uniquePatch(), null);
        mockMvc.perform(post("/api/policies/" + policyId + "/evaluate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.evaluateBody(releaseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestEvaluation.decision").value(PolicyDecision.FAILED.name()));
    }

    @Test
    void customExpressionMatchesStatus() throws Exception {
        String policyId = createPolicy(
                "custom-" + UUID.randomUUID(),
                "CUSTOM_EXPRESSION",
                "{\"requireStatus\":\"PUBLISHED\"}");
        UUID releaseId = seedPublished("82.3." + PolicyTestFixture.uniquePatch(), "ok");
        MvcResult result = mockMvc.perform(post("/api/policies/" + policyId + "/evaluate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.evaluateBody(releaseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestEvaluation.decision").value(PolicyDecision.PASSED.name()))
                .andReturn();
        assertThat(objectMapper
                        .readTree(result.getResponse().getContentAsString())
                        .get("latestEvaluation")
                        .get("evidence")
                        .isArray())
                .isTrue();
    }

    @Test
    void bestEffortConvertsFailureToWarning() throws Exception {
        String name = "best-" + UUID.randomUUID();
        String body = PolicyTestFixture.createBody(name, "RELEASE_NOTES_REQUIRED", "BEST_EFFORT", 20, "{}");
        MvcResult created = mockMvc.perform(post("/api/policies")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        String policyId = objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
        UUID releaseId = seedPublished("82.4." + PolicyTestFixture.uniquePatch(), null);
        mockMvc.perform(post("/api/policies/" + policyId + "/evaluate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.evaluateBody(releaseId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.latestEvaluation.decision").value(PolicyDecision.WARNING.name()));
    }

    private String createPolicy(String name, String type, String config) throws Exception {
        MvcResult created = mockMvc.perform(post("/api/policies")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.createBody(name, type, "ALL_REQUIRED", 10, config)))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText();
    }

    private UUID seedPublished(String version, String description) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                "Eval Rel " + version,
                description,
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 1, 0, 0, VersionBump.PATCH),
                "eval-fp-" + version + "-" + mergeId,
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
