package ai.nova.platform.rollback;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.rollback.entity.RollbackEventType;
import ai.nova.platform.rollback.entity.RollbackStatus;
import ai.nova.platform.rollback.repository.RollbackEventRepository;
import ai.nova.platform.rollback.repository.RollbackOperationRepository;
import ai.nova.platform.rollback.repository.RollbackPlanRepository;
import ai.nova.platform.rollback.repository.RollbackTargetRepository;
import ai.nova.platform.rollback.repository.RollbackValidationRepository;
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
class RollbackStorageTest {

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

    @Autowired
    private RollbackOperationRepository operationRepository;

    @Autowired
    private RollbackPlanRepository planRepository;

    @Autowired
    private RollbackTargetRepository targetRepository;

    @Autowired
    private RollbackEventRepository eventRepository;

    @Autowired
    private RollbackValidationRepository validationRepository;

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
    void persistsPlanTargetsEventsAndValidations() throws Exception {
        UUID targetId = seedPublished("81.20." + uniquePatch());
        UUID currentId = seedPublished("81.21." + uniquePatch());
        UUID deploymentId = observe(currentId, "STAGING");
        String body = RollbackTestFixture.createBody(
                currentId, deploymentId, targetId, "STAGING", "PREVIOUS_RELEASE", "storage");

        MvcResult created = mockMvc.perform(post("/api/rollbacks/create")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andReturn();
        UUID id = UUID.fromString(objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());

        assertThat(operationRepository.findById(id)).isPresent();
        assertThat(operationRepository.findById(id).orElseThrow().getStatus()).isEqualTo(RollbackStatus.DRAFT);
        assertThat(planRepository.findByRollbackOperationId(id)).isPresent();
        assertThat(targetRepository.findByRollbackOperationIdOrderBySortOrderAsc(id)).hasSize(1);
        assertThat(eventRepository.findByRollbackOperationIdOrderByCreatedAtAsc(id))
                .anyMatch(e -> e.getEventType() == RollbackEventType.CREATED);

        mockMvc.perform(post("/api/rollbacks/" + id + "/validate")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk());

        assertThat(validationRepository.findByRollbackOperationIdOrderByCreatedAtAsc(id)).isNotEmpty();
        assertThat(planRepository.findByRollbackOperationId(id).orElseThrow().isImmutable()).isTrue();
    }

    private UUID observe(UUID releaseId, String environment) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/deployments/observe")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(DeploymentTestFixture.observeBody(
                                releaseId,
                                environment,
                                "LOCAL",
                                "sto-" + UUID.randomUUID(),
                                "SUCCEEDED",
                                "HEALTHY")))
                .andExpect(status().isOk())
                .andReturn();
        return UUID.fromString(objectMapper.readTree(result.getResponse().getContentAsString()).get("id").asText());
    }

    private UUID seedPublished(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                RollbackTestFixture.ORG_ID,
                RollbackTestFixture.PROJECT_ID,
                "RB Sto " + version,
                "storage",
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 1, 0, 0, VersionBump.PATCH),
                "rb-sto-fp-" + version + "-" + mergeId,
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
        return draft.getId();
    }

    private static long uniquePatch() {
        return Math.floorMod(UUID.randomUUID().getLeastSignificantBits(), 900_000L) + 100_000L;
    }
}
