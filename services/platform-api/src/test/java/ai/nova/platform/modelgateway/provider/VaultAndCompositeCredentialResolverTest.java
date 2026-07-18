package ai.nova.platform.modelgateway.provider;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import ai.nova.platform.modelgateway.secrets.vault.ProviderSecretService;
import ai.nova.platform.modelgateway.validation.CredentialReferenceValidator;

class VaultAndCompositeCredentialResolverTest {

    @Test
    void vaultResolverDecryptsActiveSecret() {
        ProviderSecretService secretService = mock(ProviderSecretService.class);
        UUID orgId = UUID.randomUUID();
        UUID secretId = UUID.randomUUID();
        when(secretService.resolveActiveSecret(org.mockito.ArgumentMatchers.eq(secretId), org.mockito.ArgumentMatchers.any()))
                .thenReturn(Optional.empty());
        when(secretService.resolveActiveSecret(secretId, orgId)).thenReturn(Optional.of("decrypted-value"));

        VaultProviderCredentialResolver vault =
                new VaultProviderCredentialResolver(secretService, new CredentialReferenceValidator());
        assertThat(vault.resolve("vault:provider-secret:" + secretId, orgId)).contains("decrypted-value");
        assertThat(vault.resolve("vault:provider-secret:" + secretId, UUID.randomUUID())).isEmpty();
        assertThat(vault.resolve("env:NOVA_PROVIDER_X", orgId)).isEmpty();
    }

    @Test
    void compositePrefersVaultThenEnv() {
        VaultProviderCredentialResolver vault = mock(VaultProviderCredentialResolver.class);
        EnvironmentProviderCredentialResolver env = mock(EnvironmentProviderCredentialResolver.class);
        UUID orgId = UUID.randomUUID();
        String vaultRef = "vault:provider-secret:" + UUID.randomUUID();
        when(vault.resolve(vaultRef, orgId)).thenReturn(Optional.of("from-vault"));
        when(env.resolve(vaultRef, orgId)).thenReturn(Optional.of("from-env"));

        CompositeProviderCredentialResolver composite = new CompositeProviderCredentialResolver(vault, env);
        assertThat(composite.resolve(vaultRef, orgId)).contains("from-vault");

        String envRef = "env:NOVA_PROVIDER_OPENAI";
        when(vault.resolve(envRef, orgId)).thenReturn(Optional.empty());
        when(env.resolve(envRef, orgId)).thenReturn(Optional.of("from-env"));
        assertThat(composite.resolve(envRef, orgId)).contains("from-env");
    }
}
