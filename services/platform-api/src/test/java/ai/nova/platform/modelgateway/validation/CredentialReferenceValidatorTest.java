package ai.nova.platform.modelgateway.validation;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.web.error.ApiException;

class CredentialReferenceValidatorTest {

    private final CredentialReferenceValidator validator = new CredentialReferenceValidator();

    @Test
    void acceptsEnvReference() {
        assertThatCode(() -> validator.validate("env:NOVA_PROVIDER_OPENAI", AiProviderType.OPENAI))
                .doesNotThrowAnyException();
    }

    @Test
    void acceptsVaultReference() {
        assertThatCode(() -> validator.validate(
                        "vault:provider-secret:" + UUID.randomUUID(), AiProviderType.OPENAI))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsPlaintextSecret() {
        assertThatThrownBy(() -> validator.validate("sk-abcdefghijklmnopqrstuvwxyz123456", AiProviderType.OPENAI))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void rejectsBearerToken() {
        assertThatThrownBy(() -> validator.validate("Bearer abcdef", AiProviderType.OPENAI))
                .isInstanceOf(ApiException.class);
    }

    @Test
    void deterministicLocalRequiresNullCredential() {
        assertThatThrownBy(() -> validator.validate("env:NOVA_PROVIDER_X", AiProviderType.DETERMINISTIC_LOCAL))
                .isInstanceOf(ApiException.class);
    }
}
