package ai.nova.platform.deployment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import ai.nova.platform.agent.runtime.AgentRuntimeClient;
import ai.nova.platform.deployment.entity.DeploymentHealthLevel;
import ai.nova.platform.deployment.entity.DeploymentStatus;
import ai.nova.platform.deployment.repository.DeploymentEnvironmentRepository;
import ai.nova.platform.deployment.service.DeploymentStorageService;
import ai.nova.platform.deployment.support.DeploymentTestFixture;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;

@SpringBootTest
class DeploymentStorageTest {

    @Autowired
    private DeploymentStorageService storageService;

    @Autowired
    private DeploymentEnvironmentRepository environmentRepository;

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
    void persistsObservationArtifactsHealthAndTimeline() {
        UUID releaseId = seedPublished(DeploymentTestFixture.uniqueVersion("store"));
        var env = environmentRepository.findByCodeIgnoreCase("PRODUCTION").orElseThrow();
        Instant start = Instant.parse("2026-07-19T10:00:00Z");
        Instant end = Instant.parse("2026-07-19T10:05:00Z");
        String hash = DeploymentStorageService.deploymentHash(releaseId, "PRODUCTION", "store-1", "LOCAL", start);

        var entity = storageService.createObserved(
                DeploymentTestFixture.ORG_ID,
                DeploymentTestFixture.PROJECT_ID,
                releaseId,
                env,
                null,
                "80.0.1",
                "manifest",
                DeploymentStatus.RUNNING,
                DeploymentHealthLevel.WARNING,
                "cpu high",
                "LOCAL",
                "store-1",
                hash,
                DeploymentTestFixture.USER_ID,
                start,
                end,
                "logs",
                List.of(new ai.nova.platform.deployment.dto.DeploymentDtos.ArtifactRef(
                        "IMAGE", "memory://x", "h", "app")));

        var dto = storageService.toDto(entity);
        assertThat(dto.durationMs()).isEqualTo(300_000L);
        assertThat(dto.artifacts()).hasSize(1);
        assertThat(dto.healthHistory()).isNotEmpty();
        assertThat(dto.timeline().get(0).eventType()).isEqualTo("OBSERVED");

        storageService.updateStatus(entity.getId(), DeploymentStatus.SUCCEEDED, DeploymentHealthLevel.HEALTHY, "recovered");
        var updated = storageService.toDto(storageService.require(entity.getId()));
        assertThat(updated.status()).isEqualTo(DeploymentStatus.SUCCEEDED);
        assertThat(updated.health()).isEqualTo(DeploymentHealthLevel.HEALTHY);
    }

    private UUID seedPublished(String version) {
        UUID mergeId = UUID.randomUUID();
        var draft = releaseStorageService.createDraft(
                DeploymentTestFixture.ORG_ID,
                DeploymentTestFixture.PROJECT_ID,
                "Storage Rel " + version,
                null,
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion(version, 3, 0, 0, VersionBump.PATCH),
                "storage-fp-" + version + "-" + mergeId,
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
        return draft.getId();
    }
}
