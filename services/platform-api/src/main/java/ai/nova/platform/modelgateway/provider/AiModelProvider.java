package ai.nova.platform.modelgateway.provider;

public interface AiModelProvider {

    String adapterKey();

    ProviderCapabilities capabilities();

    ProviderInvokeResult invoke(ProviderInvokeRequest request) throws ProviderException;
}
