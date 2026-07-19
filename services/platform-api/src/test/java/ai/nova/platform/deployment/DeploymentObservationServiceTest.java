package ai.nova.platform.deployment;

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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deployment.entity.DeploymentHealthLevel;
import ai.nova.platform.deployment.entity.DeploymentStatus;
import ai.nova.platform.deployment.support.DeploymentTestFixture;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.security.JwtService;

@SpringBootTest(properties = {"nova.deployment.enabled=true", "nova.release.enabled=true"})
@AutoConfigureMockMvc
class DeploymentObservationServiceTest {

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
        doAnswer(invocation -> null).when(agentRuntimeClient).createOrUpdateAgentDefinition(
                any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
        accessToken = jwtService.createAccessToken(DeploymentTestFixture.deploymentAdminUser());
    }

    @Test
    void observeVerifyAndIdempotentReturn() throws Exception {
        UUID releaseId = seedPublishedRelease(DeploymentTestFixture.uniqueVersion("obs"));
        String key = "ext-" + UUID.randomUUID();
        String body = DeploymentTestFixture.observeBody(
                releaseId, "STAGING", "GITHUB_ACTIONS", key, "RUNNING", "HEALTHY");

        MvcResult first = mockMvc.perform(post("/api/deployments/observe")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DeploymentStatus.RUNNING.name()))
                .andExpect(jsonPath("$.health").value(DeploymentHealthLevel.HEALTHY.name()))
                .andExpect(jsonPath("$.environmentCode").value("STAGING"))
                .andExpect(jsonPath("$.semanticVersion").isNotEmpty())
                .andReturn();

        String id = objectMapper.readTree(first.getResponse().getContentAsString()).get("id").asText();

        MvcResult second = mockMvc.perform(post("/api/deployments/observe")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        assertThat(objectMapper.readTree(second.getResponse().getContentAsString()).get("id").asText()).isEqualTo(id);

        mockMvc.perform(post("/api/deployments/" + id + "/verify")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(DeploymentStatus.SUCCEEDED.name()));

        mockMvc.perform(get("/api/deployments/" + id + "/history")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.timeline").isArray());
    }

    @Test
    void releaseNotFoundFails() throws Exception {
        mockMvc.perform(post("/api/deployments/observe")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DeploymentTestFixture.observeBody(
                                UUID.randomUUID(), "QA", "LOCAL", "k1", "PENDING", "UNKNOWN")))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("DEPLOYMENT_RELEASE_NOT_FOUND"));
    }

    @Test
    void unknownEnvironmentFails() throws Exception {
        UUID releaseId = seedPublishedRelease(DeploymentTestFixture.uniqueVersion("env"));
        mockMvc.perform(post("/api/deployments/observe")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DeploymentTestFixture.observeBody(
                                releaseId, "NOPE", "LOCAL", "k2", "PENDING", "UNKNOWN")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("DEPLOYMENT_ENVIRONMENT_UNKNOWN"));
    }

    private UUID seedPublishedRelease(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                DeploymentTestFixture.ORG_ID,
                DeploymentTestFixture.PROJECT_ID,
                "Deploy Rel " + version,
                "for deployment tests",
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 1, 0, 0, VersionBump.PATCH),
                "deploy-fp-" + version + "-" + mergeId,
                DeploymentTestFixture.USER_ID,
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
}
