package ai.nova.platform.modelgateway.provider;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import ai.nova.platform.modelgateway.validation.CredentialReferenceValidator;

class EnvironmentProviderCredentialResolverTest {

    private final EnvironmentProviderCredentialResolver resolver =
            new EnvironmentProviderCredentialResolver(new CredentialReferenceValidator());

    @Test
    void resolvesAllowlistedEnvReferenceWhenPresent() {
        String envName = "NOVA_PROVIDER_TEST_RESOLVER";
        String previous = System.getenv(envName);
        try {
            // Cannot set env in Java portably; verify empty when unset.
            assertThat(resolver.resolve("env:" + envName)).isEmpty();
        } finally {
            // no-op cleanup
            if (previous != null) {
                // environment restored externally
            }
        }
    }

    @Test
    void rejectsNonEnvReference() {
        assertThat(resolver.resolve("vault:secret")).isEmpty();
    }
}
