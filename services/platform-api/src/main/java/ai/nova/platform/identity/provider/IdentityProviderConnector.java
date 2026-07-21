package ai.nova.platform.identity.provider;

import ai.nova.platform.identity.entity.ProviderType;

public interface IdentityProviderConnector {

    boolean supports(ProviderType type);

    AuthenticationResult authenticate(AuthenticationCredentials credentials);
}
