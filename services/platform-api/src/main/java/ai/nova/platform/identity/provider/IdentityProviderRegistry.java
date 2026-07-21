package ai.nova.platform.identity.provider;

import java.util.List;

import org.springframework.stereotype.Component;

import ai.nova.platform.identity.entity.ProviderType;

@Component
public class IdentityProviderRegistry {

    private final List<IdentityProviderConnector> connectors;

    public IdentityProviderRegistry(List<IdentityProviderConnector> connectors) {
        this.connectors = connectors;
    }

    public IdentityProviderConnector resolve(ProviderType type) {
        return connectors.stream()
                .filter(connector -> connector.supports(type))
                .findFirst()
                .orElseThrow(() -> new IllegalStateException("No connector for provider type: " + type));
    }
}
