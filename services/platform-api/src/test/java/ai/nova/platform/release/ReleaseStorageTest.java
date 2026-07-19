package ai.nova.platform.release;

import static org.assertj.core.api.Assertions.assertThat;
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
import ai.nova.platform.release.dto.ReleaseDtos.Release;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseStorageService;
import ai.nova.platform.release.service.ReleaseStorageService.ArtifactSpec;
import ai.nova.platform.release.service.ReleaseStorageService.ContentSpec;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.release.support.ReleaseTestFixture;

@SpringBootTest
class ReleaseStorageTest {

    @Autowired
    private ReleaseStorageService storageService;

    @Autowired
    private ReleaseManifestService manifestService;

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
    void persistsDraftContentsAndMarksReady() {
        UUID mergeId = UUID.randomUUID();
        var entity = storageService.createDraft(
                ReleaseTestFixture.ORG_ID,
                ReleaseTestFixture.PROJECT_ID,
                "Storage Release",
                "desc",
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                new ResolvedVersion("8." + Math.abs(mergeId.getLeastSignificantBits() % 100) + ".0", 8, 0, 0, VersionBump.PATCH),
                "fp-" + mergeId,
                ReleaseTestFixture.USER_ID,
                List.of(
                        new ContentSpec(ReleaseContentType.MERGE_OPERATION, mergeId, null),
                        new ContentSpec(ReleaseContentType.COMMIT, null, "deadbeef")),
                List.of(new ArtifactSpec("REF", "memory://x", "h", "label")));

        assertThat(entity.getStatus()).isEqualTo(ReleaseStatus.DRAFT);
        Release dto = storageService.toDto(entity);
        assertThat(dto.contents()).hasSize(2);
        assertThat(dto.artifacts()).hasSize(1);
        assertThat(dto.version().semanticVersion()).startsWith("8.");

        storageService.markPreparing(entity.getId());
        var manifest = manifestService.build(
                storageService.require(entity.getId()),
                storageService.contents(entity.getId()),
                storageService.artifacts(entity.getId()));
        Release ready = storageService.toDto(storageService.markReady(entity.getId(), manifest));
        assertThat(ready.status()).isEqualTo(ReleaseStatus.READY);
        assertThat(ready.manifestHash()).isEqualTo(manifest.manifestHash());
        assertThat(ready.timeline().stream().map(t -> t.eventType())).contains("READY", "MANIFEST_GENERATED");
        assertThat(ready.preparedAt()).isNotNull();
    }
}
