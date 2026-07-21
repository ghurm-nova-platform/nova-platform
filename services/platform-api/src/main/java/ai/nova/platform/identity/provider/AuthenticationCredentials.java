package ai.nova.platform.identity.provider;

import ai.nova.platform.identity.entity.ProviderType;

public record AuthenticationCredentials(
        String email,
        String password,
        String organizationSlug) {
}
