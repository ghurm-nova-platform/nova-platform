package ai.nova.platform.modelgateway.provider;

import java.util.Optional;
import java.util.UUID;

import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
public class CompositeProviderCredentialResolver implements ProviderCredentialResolver {

    private final VaultProviderCredentialResolver vaultResolver;
    private final EnvironmentProviderCredentialResolver environmentResolver;

    public CompositeProviderCredentialResolver(
            VaultProviderCredentialResolver vaultResolver,
            EnvironmentProviderCredentialResolver environmentResolver) {
        this.vaultResolver = vaultResolver;
        this.environmentResolver = environmentResolver;
    }

    @Override
    public Optional<String> resolve(String credentialReference, UUID organizationId) {
        Optional<String> vault = vaultResolver.resolve(credentialReference, organizationId);
        if (vault.isPresent()) {
            return vault;
        }
        return environmentResolver.resolve(credentialReference, organizationId);
    }
}
