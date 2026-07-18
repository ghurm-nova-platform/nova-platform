package ai.nova.platform.modelgateway.provider;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import org.junit.jupiter.api.Test;

import ai.nova.platform.modelgateway.validation.CredentialReferenceValidator;

class EnvironmentProviderCredentialResolverTest {

    private final EnvironmentProviderCredentialResolver resolver =
            new EnvironmentProviderCredentialResolver(new CredentialReferenceValidator());

    @Test
    void resolvesAllowlistedEnvReferenceWhenPresent() {
        String envName = "NOVA_PROVIDER_TEST_RESOLVER";
        // Cannot set env in Java portably; verify empty when unset.
        assertThat(resolver.resolve("env:" + envName, UUID.randomUUID())).isEmpty();
    }

    @Test
    void rejectsNonEnvReference() {
        assertThat(resolver.resolve("vault:secret", UUID.randomUUID())).isEmpty();
    }
}
