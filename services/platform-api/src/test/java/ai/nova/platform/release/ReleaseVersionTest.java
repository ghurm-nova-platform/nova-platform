package ai.nova.platform.release;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import ai.nova.platform.release.entity.VersionBump;
import ai.nova.platform.release.service.ReleaseVersionService;
import ai.nova.platform.release.service.ReleaseVersionService.ResolvedVersion;
import ai.nova.platform.release.support.ReleaseTestFixture;

@SpringBootTest
class ReleaseVersionTest {

    @Autowired
    private ReleaseVersionService versionService;

    @Test
    void firstPatchStartsAt001() {
        ResolvedVersion resolved = versionService.resolve(
                ReleaseTestFixture.ORG_ID, UUID.randomUUID(), VersionBump.PATCH, null);
        assertThat(resolved.semanticVersion()).isEqualTo("0.0.1");
        assertThat(resolved.major()).isZero();
        assertThat(resolved.minor()).isZero();
        assertThat(resolved.patch()).isEqualTo(1);
    }

    @Test
    void firstMinorStartsAt010() {
        ResolvedVersion resolved = versionService.resolve(
                ReleaseTestFixture.ORG_ID, UUID.randomUUID(), VersionBump.MINOR, null);
        assertThat(resolved.semanticVersion()).isEqualTo("0.1.0");
    }

    @Test
    void firstMajorStartsAt100() {
        ResolvedVersion resolved = versionService.resolve(
                ReleaseTestFixture.ORG_ID, UUID.randomUUID(), VersionBump.MAJOR, null);
        assertThat(resolved.semanticVersion()).isEqualTo("1.0.0");
    }

    @Test
    void explicitVersionIsParsed() {
        ResolvedVersion resolved = versionService.resolve(
                ReleaseTestFixture.ORG_ID, UUID.randomUUID(), VersionBump.PATCH, "2.3.4");
        assertThat(resolved.semanticVersion()).isEqualTo("2.3.4");
        assertThat(List.of(resolved.major(), resolved.minor(), resolved.patch())).containsExactly(2, 3, 4);
    }

    @Test
    void parseRejectsInvalidSemver() {
        org.assertj.core.api.Assertions.assertThatThrownBy(() -> versionService.parse("1.2"))
                .hasMessageContaining("MAJOR.MINOR.PATCH");
    }
}
