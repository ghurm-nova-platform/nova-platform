package ai.nova.platform.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.release.entity.ReleaseArtifactEntity;
import ai.nova.platform.release.entity.ReleaseContentEntity;
import ai.nova.platform.release.entity.ReleaseContentType;
import ai.nova.platform.release.entity.ReleaseOperationEntity;
import ai.nova.platform.release.entity.ReleaseStatus;
import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.entity.VersionStrategy;
import ai.nova.platform.release.service.ReleaseManifestService;
import ai.nova.platform.release.service.ReleaseManifestService.ManifestResult;
import ai.nova.platform.release.support.ReleaseTestFixture;

@SpringBootTest
class ReleaseManifestTest {

    @Autowired
    private ReleaseManifestService manifestService;

    @Test
    void manifestHashIsStableForIdenticalInputs() {
        UUID releaseId = UUID.randomUUID();
        UUID mergeId = UUID.randomUUID();
        ReleaseOperationEntity release = sampleRelease(releaseId);
        List<ReleaseContentEntity> contents = List.of(
                new ReleaseContentEntity(UUID.randomUUID(), releaseId, ReleaseContentType.MERGE_OPERATION, mergeId, null, 0, Instant.now()),
                new ReleaseContentEntity(UUID.randomUUID(), releaseId, ReleaseContentType.COMMIT, null, "abc123", 1, Instant.now()));
        List<ReleaseArtifactEntity> artifacts = List.of(
                new ReleaseArtifactEntity(UUID.randomUUID(), releaseId, "REF", "memory://a", "hash1", "a", Instant.now()));

        ManifestResult first = manifestService.build(release, contents, artifacts);
        ManifestResult second = manifestService.build(release, contents, artifacts);

        assertThat(first.manifestHash()).isEqualTo(second.manifestHash());
        assertThat(first.manifestHash()).hasSize(64);
        assertThat(first.manifestJson()).contains(mergeId.toString());
        assertThat(first.manifestJson()).contains("abc123");
        assertThat(first.manifestJson()).contains(release.getSemanticVersion());
    }

    @Test
    void contentFingerprintIgnoresOrdering() {
        UUID a = UUID.fromString("11111111-1111-1111-1111-111111111111");
        UUID b = UUID.fromString("22222222-2222-2222-2222-222222222222");
        String left = manifestService.contentFingerprint(
                List.of(b, a), List.of(), List.of(), List.of(), List.of("z", "a"), List.of());
        String right = manifestService.contentFingerprint(
                List.of(a, b), List.of(), List.of(), List.of(), List.of("a", "z"), List.of());
        assertThat(left).isEqualTo(right);
    }

    private static ReleaseOperationEntity sampleRelease(UUID releaseId) {
        return new ReleaseOperationEntity(
                releaseId,
                ReleaseTestFixture.ORG_ID,
                ReleaseTestFixture.PROJECT_ID,
                1L,
                "1.0.0",
                "Release One",
                "desc",
                ReleaseStatus.DRAFT,
                VersionStrategy.SEMVER,
                VersionBump.PATCH,
                "fingerprint",
                ReleaseTestFixture.USER_ID,
                Instant.parse("2026-07-19T12:00:00Z"));
    }
}
