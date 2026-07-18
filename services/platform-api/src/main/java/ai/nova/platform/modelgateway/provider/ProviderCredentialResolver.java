package ai.nova.platform.modelgateway.provider;

import java.util.Optional;
import java.util.UUID;

public interface ProviderCredentialResolver {

    Optional<String> resolve(String credentialReference, UUID organizationId);
}
