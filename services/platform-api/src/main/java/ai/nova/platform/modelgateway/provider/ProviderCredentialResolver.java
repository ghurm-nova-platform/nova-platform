package ai.nova.platform.modelgateway.provider;

import java.util.Optional;

public interface ProviderCredentialResolver {

    Optional<String> resolve(String credentialReference);
}
