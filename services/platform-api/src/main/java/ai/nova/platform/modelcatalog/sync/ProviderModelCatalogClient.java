package ai.nova.platform.modelcatalog.sync;

import java.util.List;

import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.provider.ProviderException;

public interface ProviderModelCatalogClient {

    boolean supports(AiProviderType type);

    List<DiscoveredModelDescriptor> discoverModels(AiProvider provider, String credential) throws ProviderException;
}
