package ai.nova.platform.rollback;

import static org.assertj.core.api.Assertions.assertThat;
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
import ai.nova.platform.deployment.support.DeploymentTestFixture;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.rollback.entity.RollbackStatus;
import ai.nova.platform.rollback.support.RollbackTestFixture;
import ai.nova.platform.security.JwtService;

@SpringBootTest(
        properties = {
            "nova.rollback.enabled=true",
            "nova.rollback.execution-enabled=false",
            "nova.deployment.enabled=true",
            "nova.release.enabled=true"
        })
@AutoConfigureMockMvc
class RollbackManagerServiceTest {

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
        accessToken = jwtService.createAccessToken(RollbackTestFixture.rollbackAdminUser());
    }

    @Test
    void createValidateAndIdempotentReturn() throws Exception {
        UUID targetId = seedPublishedRelease("81.1." + uniquePatch());
        UUID currentId = seedPublishedRelease("81.2." + uniquePatch());
        UUID deploymentId = observeDeployment(currentId, "STAGING");

        String body = RollbackTestFixture.createBody(
                currentId, deploymentId, targetId, "STAGING", "PREVIOUS_RELEASE", "rollback after regression");

        MvcResult first = mockMvc.perform(post("/api/rollbacks/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RollbackStatus.DRAFT.name()))
                .andExpect(jsonPath("$.rollbackPlanHash").isNotEmpty())
                .andReturn();
        String id = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();

        MvcResult second = mockMvc.perform(post("/api/rollbacks/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText()).isEqualTo(id);

        mockMvc.perform(post("/api/rollbacks/" + id + "/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(RollbackStatus.READY.name()))
                .andExpect(jsonPath("$.plan.immutable").value(true))
                .andExpect(jsonPath("$.plan.validationResult").value("PASSED"));

        mockMvc.perform(get("/api/rollbacks/" + id + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    void deploymentNotFoundFails() throws Exception {
        UUID releaseId = seedPublishedRelease("81.3." + uniquePatch());
        UUID targetId = seedPublishedRelease("81.0." + uniquePatch());
        mockMvc.perform(post("/api/rollbacks/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(RollbackTestFixture.createBody(
                                releaseId, UUID.randomUUID(), targetId, "QA", "SPECIFIC_RELEASE", "missing deploy")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("ROLLBACK_DEPLOYMENT_NOT_FOUND"));
    }

    private UUID observeDeployment(UUID releaseId, String environment) throws Exception {
        String key = "rb-dep-" + UUID.randomUUID();
        MvcResult result = mockMvc.perform(post("/api/deployments/observe")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DeploymentTestFixture.observeBody(
                                releaseId, environment, "LOCAL", key, "SUCCEEDED", "HEALTHY")))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID seedPublishedRelease(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                RollbackTestFixture.ORG_ID,
                RollbackTestFixture.PROJECT_ID,
                "Rollback Rel " + version,
                "for rollback tests",
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 1, 0, 0, VersionBump.PATCH),
                "rb-fp-" + version + "-" + mergeId,
                RollbackTestFixture.USER_ID,
                List.of(new ContentSpec(ReleaseContentType.MERGE_OPERATION, mergeId, null)),
                List.of());
        releaseStorageService.markPreparing(draft.getId());
        var manifest = releaseManifestService.build(
                releaseStorageService.require(draft.getId()),
                releaseStorageService.contents(draft.getId()),
                releaseStorageService.artifacts(draft.getId()));
        releaseStorageService.markReady(draft.getId(), manifest);
        releaseStorageService.markPublished(draft.getId());
        assertThat(releaseStorageService.require(draft.getId()).getStatus()).isEqualTo(ReleaseStatus.PUBLISHED);
        return draft.getId();
    }

    private static long uniquePatch() {
        return Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
    }
}
