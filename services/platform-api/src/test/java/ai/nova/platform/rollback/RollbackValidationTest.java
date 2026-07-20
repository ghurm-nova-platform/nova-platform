package ai.nova.platform.rollback;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ai.nova.platform.rollback.entity.RollbackStrategy;
import ai.nova.platform.rollback.service.RollbackManagerService;

class RollbackValidationTest {

    @Test
    void previousStrategiesRequireOlderTarget() {
        assertThat(RollbackManagerService.isVersionCompatible(
                        RollbackStrategy.PREVIOUS_RELEASE, "2.0.0", "1.9.9"))
                .isTrue();
        assertThat(RollbackManagerService.isVersionCompatible(
                        RollbackStrategy.PREVIOUS_STABLE, "2.0.0", "2.1.0"))
                .isFalse();
        assertThat(RollbackManagerService.isVersionCompatible(
                        RollbackStrategy.HOTFIX_ONLY, "2.0.1", "2.0.0"))
                .isTrue();
    }

    @Test
    void specificAndCustomAllowAnyDifferentVersion() {
        assertThat(RollbackManagerService.isVersionCompatible(
                        RollbackStrategy.SPECIFIC_RELEASE, "1.0.0", "3.0.0"))
                .isTrue();
        assertThat(RollbackManagerService.isVersionCompatible(
                        RollbackStrategy.CUSTOM, "1.0.0", "1.0.0"))
                .isFalse();
    }

    @Test
    void comparesSemVerCore() {
        assertThat(RollbackManagerService.compareSemVer("1.2.3", "1.2.4")).isNegative();
        assertThat(RollbackManagerService.compareSemVer("2.0.0", "1.9.9")).isPositive();
        assertThat(RollbackManagerService.compareSemVer("1.0.0-rc.1", "1.0.0")).isZero();
    }
}
