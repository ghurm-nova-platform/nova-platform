package ai.nova.platform.modelgateway.provider;

public record ProviderCapabilities(
        boolean tools,
        boolean knowledgeContext,
        boolean jsonOutput,
        boolean systemMessages,
        boolean streaming) {
}
