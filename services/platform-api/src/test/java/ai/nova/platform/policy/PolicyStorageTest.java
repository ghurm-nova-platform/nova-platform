package ai.nova.platform.policy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.policy.entity.EvaluationMode;
import ai.nova.platform.policy.entity.PolicyDecision;
import ai.nova.platform.policy.entity.PolicyEventType;
import ai.nova.platform.policy.entity.PolicyStatus;
import ai.nova.platform.policy.entity.PolicyType;
import ai.nova.platform.policy.entity.ReleasePolicyEntity;
import ai.nova.platform.policy.repository.PolicyEventRepository;
import ai.nova.platform.policy.repository.PolicyEvidenceRepository;
import ai.nova.platform.policy.repository.PolicyVersionRepository;
import ai.nova.platform.policy.service.PolicyHashService;
import ai.nova.platform.policy.service.PolicyStorageService;
import ai.nova.platform.policy.support.PolicyTestFixture;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;

@SpringBootTest(properties = {"nova.policy.enabled=true", "nova.release.enabled=true"})
@AutoConfigureMockMvc
@Transactional
class PolicyStorageTest {

    @Autowired
    private PolicyStorageService storageService;

    @Autowired
    private PolicyHashService hashService;

    @Autowired
    private PolicyVersionRepository versionRepository;

    @Autowired
    private PolicyEventRepository eventRepository;

    @Autowired
    private PolicyEvidenceRepository evidenceRepository;

    @Autowired
    private ReleaseStorageService releaseStorageService;

    @Autowired
    private ReleaseManifestService releaseManifestService;

    @MockitoBean
    private AgentRuntimeClient agentRuntimeClient;

    @BeforeEach
    void setUp() {
        doAnswer(invocation -> null)
                .when(agentRuntimeClient)
                .createOrUpdateAgentDefinition(any(), any(), any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).archiveAgentDefinition(any(), any(), any());
        doAnswer(invocation -> null).when(agentRuntimeClient).cancel(any());
    }

    @Test
    void createPolicyAppendsVersionAndEvents() {
        String name = "storage-" + UUID.randomUUID();
        String configJson = hashService.toConfigJson(Map.of("minApprovals", 2));
        String fingerprint = hashService.fingerprint(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                name,
                PolicyType.MINIMUM_APPROVALS,
                EvaluationMode.ALL_REQUIRED,
                50,
                Map.of("minApprovals", 2));

        ReleasePolicyEntity created = storageService.createPolicy(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                name,
                "storage test",
                PolicyType.MINIMUM_APPROVALS,
                50,
                EvaluationMode.ALL_REQUIRED,
                configJson,
                fingerprint,
                PolicyTestFixture.USER_ID);

        assertThat(created.getStatus()).isEqualTo(PolicyStatus.ACTIVE);
        assertThat(versionRepository.findByPolicyIdOrderByVersionNumberDesc(created.getId())).hasSize(1);
        assertThat(eventRepository.findByPolicyIdOrderByCreatedAtAsc(created.getId()))
                .extracting(e -> e.getEventType())
                .contains(PolicyEventType.CREATED, PolicyEventType.ENABLED);

        var dto = storageService.toDto(created);
        assertThat(dto.versions()).hasSize(1);
        assertThat(dto.timeline()).isNotEmpty();
    }

    @Test
    void appendEvidenceIsUniquePerEvaluationKey() {
        String name = "ev-store-" + UUID.randomUUID();
        String configJson = hashService.toConfigJson(Map.of());
        String fingerprint = hashService.fingerprint(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                name,
                PolicyType.MANIFEST_INTEGRITY,
                EvaluationMode.ALL_REQUIRED,
                10,
                Map.of());
        ReleasePolicyEntity policy = storageService.createPolicy(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                name,
                null,
                PolicyType.MANIFEST_INTEGRITY,
                10,
                EvaluationMode.ALL_REQUIRED,
                configJson,
                fingerprint,
                PolicyTestFixture.USER_ID);
        var version = versionRepository.findFirstByPolicyIdOrderByVersionNumberDesc(policy.getId()).orElseThrow();
        UUID releaseId = seedPublished("82.7." + PolicyTestFixture.uniquePatch());
        Instant now = Instant.now();
        var evaluation = storageService.createEvaluation(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                policy.getId(),
                version.getId(),
                releaseId,
                PolicyDecision.PASSED,
                hashService.evaluationHash(
                        policy.getId(), version.getId(), releaseId, "1.0.0", "hash", "fp", configJson),
                "ok",
                PolicyTestFixture.USER_ID,
                now);

        storageService.appendEvidence(
                evaluation.getId(), "manifest-integrity", "RELEASE", null, true, "present", now);
        storageService.appendEvidence(
                evaluation.getId(), "manifest-integrity", "RELEASE", null, true, "duplicate", now);

        assertThat(evidenceRepository.findByPolicyEvaluationIdOrderByCreatedAtAsc(evaluation.getId()))
                .hasSize(1);
    }

    private UUID seedPublished(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                PolicyTestFixture.ORG_ID,
                PolicyTestFixture.PROJECT_ID,
                "Store Rel " + version,
                "storage",
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 1, 0, 0, VersionBump.PATCH),
                "store-fp-" + version + "-" + mergeId,
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
