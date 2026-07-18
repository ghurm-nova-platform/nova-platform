package ai.nova.platform.modelgateway.secrets.vault;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

class ProviderSecretRotationKeyTest {

    @Test
    void retiredSecretKeyNeverExceedsHundredCharacters() {
        String longKey = "A" + "B".repeat(98);
        assertThat(longKey).hasSize(99);
        String retired = ProviderSecretService.retiredSecretKey(longKey, UUID.randomUUID());
        assertThat(retired).hasSizeLessThanOrEqualTo(100);
        assertThat(retired).contains("__ROTATED_");
    }
}
