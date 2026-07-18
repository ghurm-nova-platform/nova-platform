package ai.nova.platform.modelgateway.provider;

import ai.nova.platform.modelgateway.entity.EndpointProfile;

public record ProviderEndpointConfig(
        EndpointProfile endpointProfile,
        String azureResourceName,
        String azureApiVersion) {
}
