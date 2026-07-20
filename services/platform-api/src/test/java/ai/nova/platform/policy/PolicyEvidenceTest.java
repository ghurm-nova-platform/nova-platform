package ai.nova.platform.policy;

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
import ai.nova.platform.policy.repository.PolicyEvidenceRepository;
import ai.nova.platform.policy.repository.PolicyEvaluationRepository;
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
class PolicyEvidenceTest {

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
    private PolicyEvaluationRepository evaluationRepository;

    @Autowired
    private PolicyEvidenceRepository evidenceRepository;

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
    void evidenceIsAppendOnlyAndNotDuplicatedOnIdempotentEvaluate() throws Exception {
        MvcResult created = mockMvc.perform(post("/api/policies")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.createBody(
                                "ev-" + UUID.randomUUID(),
                                "MANIFEST_INTEGRITY",
                                "ALL_REQUIRED",
                                5,
                                "{}")))
                .andExpect(status().isOk())
                .andReturn();
        UUID policyId = UUID.fromString(
                objectMapper.readTree(created.getResponse().getContentAsString()).get("id").asText());
        UUID releaseId = seedPublished("82.5." + PolicyTestFixture.uniquePatch());

        mockMvc.perform(post("/api/policies/" + policyId + "/evaluate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.evaluateBody(releaseId)))
                .andExpect(status().isOk());

        var evaluation = evaluationRepository.findFirstByPolicyIdOrderByCreatedAtDesc(policyId).orElseThrow();
        int firstCount = evidenceRepository.findByPolicyEvaluationIdOrderByCreatedAtAsc(evaluation.getId()).size();
        assertThat(firstCount).isGreaterThan(0);

        mockMvc.perform(post("/api/policies/" + policyId + "/evaluate")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(PolicyTestFixture.evaluateBody(releaseId)))
                .andExpect(status().isOk());

        assertThat(evaluationRepository.findByPolicyIdOrderByCreatedAtDesc(policyId)).hasSize(1);
        assertThat(evidenceRepository.findByPolicyEvaluationIdOrderByCreatedAtAsc(evaluation.getId()))
                .hasSize(firstCount);
    }

    private UUID seedPublished(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                "Ev Rel " + version,
                "evidence",
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 1, 0, 0, VersionBump.PATCH),
                "ev-fp-" + version + "-" + mergeId,
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
