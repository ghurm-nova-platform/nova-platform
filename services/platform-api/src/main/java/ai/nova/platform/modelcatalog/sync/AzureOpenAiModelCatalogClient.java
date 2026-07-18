package ai.nova.platform.modelcatalog.sync;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

import ai.nova.platform.modelcatalog.entity.AiModelCapability;
import ai.nova.platform.modelgateway.entity.AiModelType;
import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.EndpointProfile;
import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.ProviderFailureKind;
import ai.nova.platform.modelgateway.provider.error.UnifiedProviderErrorMapper;
import ai.nova.platform.modelgateway.provider.http.ProviderRestClientFactory;
import ai.nova.platform.web.error.ApiException;

/**
 * Discovers Azure OpenAI deployments only when endpoint metadata is complete.
 * Never fabricates inventory; capabilities are conservative and only applied when the
 * deployment payload includes a known underlying model id.
 */
@Component
public class AzureOpenAiModelCatalogClient implements ProviderModelCatalogClient {

    private final ProviderRestClientFactory restClientFactory;
    private final UnifiedProviderErrorMapper errorMapper;
    private final OpenAiModelCapabilityMapper capabilityMapper;

    public AzureOpenAiModelCatalogClient(
            ProviderRestClientFactory restClientFactory,
            UnifiedProviderErrorMapper errorMapper,
            OpenAiModelCapabilityMapper capabilityMapper) {
        this.restClientFactory = restClientFactory;
        this.errorMapper = errorMapper;
        this.capabilityMapper = capabilityMapper;
    }

    @Override
    public boolean supports(AiProviderType type) {
        return type == AiProviderType.AZURE_OPENAI;
    }

    @Override
    public List<DiscoveredModelDescriptor> discoverModels(AiProvider provider, String credential)
            throws ProviderException {
        if (provider.getEndpointProfile() != EndpointProfile.AZURE_OPENAI_RESOURCE
                || provider.getAzureResourceName() == null
                || provider.getAzureResourceName().isBlank()
                || provider.getAzureApiVersion() == null
                || provider.getAzureApiVersion().isBlank()) {
            throw new ApiException(
                    HttpStatus.CONFLICT,
                    "MODEL_SYNC_UNSUPPORTED",
                    "Azure OpenAI sync requires complete endpoint metadata");
        }

        int timeout = provider.getRequestTimeoutSeconds() != null ? provider.getRequestTimeoutSeconds() : 30;
        URI base = restClientFactory.resolveAzureBaseUrl(provider.getAzureResourceName());
        RestClient client = restClientFactory.create(base, timeout);
        JsonNode body;
        try {
            body = client.get()
                    .uri("/openai/deployments?api-version={apiVersion}", provider.getAzureApiVersion())
                    .accept(MediaType.APPLICATION_JSON)
                    .header("api-key", credential)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw errorMapper.map(ex);
        } catch (ApiException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ProviderException(
                    "PROVIDER_ERROR", ProviderFailureKind.TRANSIENT, "Azure deployments request failed");
        }

        List<DiscoveredModelDescriptor> discovered = new ArrayList<>();
        JsonNode data = body == null ? null : (body.has("data") ? body.get("data") : body.get("value"));
        if (data == null || !data.isArray()) {
            return discovered;
        }
        for (JsonNode item : data) {
            String deploymentId = text(item, "id");
            if (deploymentId == null) {
                deploymentId = text(item, "name");
            }
            if (deploymentId == null || deploymentId.isBlank()) {
                continue;
            }
            String underlyingModel = text(item, "model");
            if (underlyingModel == null) {
                JsonNode modelNode = item.get("properties");
                if (modelNode != null) {
                    underlyingModel = text(modelNode, "model");
                }
            }
            String family = underlyingModel != null ? capabilityMapper.mapFamily(underlyingModel) : null;
            Set<AiModelCapability> caps = underlyingModel != null
                    ? capabilityMapper.mapCapabilities(underlyingModel)
                    : Set.of();
            AiModelType type = underlyingModel != null
                    ? capabilityMapper.mapType(underlyingModel)
                    : AiModelType.CHAT;
            Integer context = underlyingModel != null ? capabilityMapper.mapContextWindow(underlyingModel) : null;
            Integer maxOut = underlyingModel != null ? capabilityMapper.mapMaxOutputTokens(underlyingModel) : null;
            discovered.add(new DiscoveredModelDescriptor(
                    deploymentId,
                    deploymentId,
                    family,
                    null,
                    type,
                    context,
                    maxOut,
                    caps));
        }
        return discovered;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }
}
