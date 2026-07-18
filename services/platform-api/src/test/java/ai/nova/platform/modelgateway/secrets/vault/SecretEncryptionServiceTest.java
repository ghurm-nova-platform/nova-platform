package ai.nova.platform.modelgateway.secrets.vault;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import ai.nova.platform.web.error.ApiException;

class SecretEncryptionServiceTest {

    @Test
    void encryptDecryptRoundTrip() {
        SecretsProperties properties = new SecretsProperties();
        properties.setMasterKey("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=");
        SecretEncryptionService service = new SecretEncryptionService(properties);

        String secret = "sk-test-secret-value-1234";
        EncryptedSecretPayload encrypted = service.encrypt(secret);
        assertThat(encrypted.ciphertext()).isNotEmpty();
        assertThat(encrypted.nonce()).hasSize(12);
        assertThat(encrypted.algorithm()).isEqualTo("AES-256-GCM");
        assertThat(service.decrypt(encrypted.ciphertext(), encrypted.nonce(), encrypted.keyVersion()))
                .isEqualTo(secret);
        assertThat(service.internalFingerprint(secret)).hasSize(64);
        assertThat(service.last4(secret)).isEqualTo("1234");
    }

    @Test
    void missingMasterKeyFailsClearly() {
        SecretsProperties properties = new SecretsProperties();
        properties.setMasterKey("");
        SecretEncryptionService service = new SecretEncryptionService(properties);
        assertThatThrownBy(() -> service.encrypt("value"))
                .isInstanceOf(ApiException.class)
                .extracting(ex -> ((ApiException) ex).getCode())
                .isEqualTo("SECRET_MASTER_KEY_MISSING");
    }
}
