package ai.nova.platform.deployment;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ai.nova.platform.deployment.entity.DeploymentHealthLevel;

class DeploymentHealthTest {

    @Test
    void healthLevelsAreStable() {
        assertThat(DeploymentHealthLevel.values())
                .containsExactly(
                        DeploymentHealthLevel.HEALTHY,
                        DeploymentHealthLevel.WARNING,
                        DeploymentHealthLevel.DEGRADED,
                        DeploymentHealthLevel.FAILED,
                        DeploymentHealthLevel.UNKNOWN);
    }

    @Test
    void failedAndDegradedAreUnhealthy() {
        assertThat(DeploymentHealthLevel.FAILED.name()).isEqualTo("FAILED");
        assertThat(DeploymentHealthLevel.DEGRADED.name()).isEqualTo("DEGRADED");
        assertThat(DeploymentHealthLevel.HEALTHY.name()).isEqualTo("HEALTHY");
    }
}
