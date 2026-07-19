package ai.nova.platform.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deployment.dto.DeploymentDtos.ObserveDeploymentRequest;
import ai.nova.platform.deployment.entity.DeploymentHealthLevel;
import ai.nova.platform.deployment.entity.DeploymentStatus;
import ai.nova.platform.deployment.service.DeploymentObservationService;
import ai.nova.platform.deployment.support.DeploymentTestFixture;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.web.error.ApiException;

@SpringBootTest(properties = {"nova.deployment.enabled=true", "nova.release.enabled=true"})
class DeploymentVerificationTest {

    @Autowired
    private DeploymentObservationService observationService;

    @Autowired
    private ReleaseStorageService releaseStorageService;

    @Autowired
    private ReleaseManifestService releaseManifestService;

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
    void degradedHealthFailsVerification() {
        UUID releaseId = seedPublished("2.0.0");
        var observed = observationService.observe(
                new ObserveDeploymentRequest(
                        releaseId,
                        "QA",
                        null,
                        DeploymentStatus.RUNNING,
                        DeploymentHealthLevel.DEGRADED,
                        "latency",
                        "LOCAL",
                        "deg-" + UUID.randomUUID(),
                        null,
                        null,
                        null,
                        List.of()),
                DeploymentTestFixture.deploymentAdminUser());

        assertThatThrownBy(() -> observationService.verify(observed.id(), DeploymentTestFixture.deploymentAdminUser()))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("DEPLOYMENT_VERIFICATION_FAILED");
    }

    @Test
    void healthyObservationVerifiesSuccessfully() {
        UUID releaseId = seedPublished("2.1.0");
        var observed = observationService.observe(
                new ObserveDeploymentRequest(
                        releaseId,
                        "TESTING",
                        null,
                        DeploymentStatus.RUNNING,
                        DeploymentHealthLevel.HEALTHY,
                        "ok",
                        "LOCAL",
                        "ok-" + UUID.randomUUID(),
                        null,
                        null,
                        null,
                        List.of()),
                DeploymentTestFixture.deploymentAdminUser());
        var verified = observationService.verify(observed.id(), DeploymentTestFixture.deploymentAdminUser());
        assertThat(verified.status()).isEqualTo(DeploymentStatus.SUCCEEDED);
        assertThat(verified.health()).isEqualTo(DeploymentHealthLevel.HEALTHY);
    }

    private UUID seedPublished(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                DeploymentTestFixture.ORG_ID,
                DeploymentTestFixture.PROJECT_ID,
                "Verify Rel " + version,
                null,
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 2, 0, 0, VersionBump.PATCH),
                "verify-fp-" + version + "-" + mergeId,
                DeploymentTestFixture.USER_ID,
                List.of(new ContentSpec(ReleaseContentType.COMMIT, null, "sha-" + version)),
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
