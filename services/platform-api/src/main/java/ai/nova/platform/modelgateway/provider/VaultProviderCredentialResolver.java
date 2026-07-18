package ai.nova.platform.modelgateway.provider;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Component;

import ai.nova.platform.modelgateway.secrets.vault.ProviderSecretService;
import ai.nova.platform.modelgateway.validation.CredentialReferenceValidator;

@Component
public class VaultProviderCredentialResolver implements ProviderCredentialResolver {

    private final ProviderSecretService providerSecretService;
    private final CredentialReferenceValidator credentialReferenceValidator;

    public VaultProviderCredentialResolver(
            ProviderSecretService providerSecretService,
            CredentialReferenceValidator credentialReferenceValidator) {
        this.providerSecretService = providerSecretService;
        this.credentialReferenceValidator = credentialReferenceValidator;
    }

    @Override
    public Optional<String> resolve(String credentialReference, UUID organizationId) {
        if (credentialReference == null || credentialReference.isBlank() || organizationId == null) {
            return Optional.empty();
        }
        credentialReferenceValidator.rejectSuspicious(credentialReference);
        Optional<UUID> secretId = ProviderSecretService.parseVaultSecretId(credentialReference);
        if (secretId.isEmpty()) {
            return Optional.empty();
        }
        return providerSecretService.resolveActiveSecret(secretId.get(), organizationId);
    }
}
