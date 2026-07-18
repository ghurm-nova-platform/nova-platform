package ai.nova.platform.modelcatalog.sync;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.databind.JsonNode;

import ai.nova.platform.modelgateway.entity.AiProvider;
import ai.nova.platform.modelgateway.entity.AiProviderType;
import ai.nova.platform.modelgateway.entity.EndpointProfile;
import ai.nova.platform.modelgateway.provider.ProviderException;
import ai.nova.platform.modelgateway.provider.error.UnifiedProviderErrorMapper;
import ai.nova.platform.modelgateway.provider.http.ProviderRestClientFactory;

@Component
public class OpenAiModelCatalogClient implements ProviderModelCatalogClient {

    private final ProviderRestClientFactory restClientFactory;
    private final UnifiedProviderErrorMapper errorMapper;
    private final OpenAiModelCapabilityMapper capabilityMapper;

    public OpenAiModelCatalogClient(
            ProviderRestClientFactory restClientFactory,
            UnifiedProviderErrorMapper errorMapper,
            OpenAiModelCapabilityMapper capabilityMapper) {
        this.restClientFactory = restClientFactory;
        this.errorMapper = errorMapper;
        this.capabilityMapper = capabilityMapper;
    }

    @Override
    public boolean supports(AiProviderType type) {
        return type == AiProviderType.OPENAI;
    }

    @Override
    public List<DiscoveredModelDescriptor> discoverModels(AiProvider provider, String credential)
            throws ProviderException {
        if (provider.getEndpointProfile() != null && provider.getEndpointProfile() != EndpointProfile.OPENAI_PUBLIC) {
            throw new ProviderException(
                    "ENDPOINT_PROFILE_INVALID",
                    ai.nova.platform.modelgateway.provider.ProviderFailureKind.PERMANENT,
                    "Invalid endpoint profile");
        }
        int timeout = provider.getRequestTimeoutSeconds() != null ? provider.getRequestTimeoutSeconds() : 30;
        URI base = restClientFactory.resolveOpenAiBaseUrl();
        RestClient client = restClientFactory.create(base, timeout);
        JsonNode body;
        try {
            body = client.get()
                    .uri("/v1/models")
                    .accept(MediaType.APPLICATION_JSON)
                    .header("Authorization", "Bearer " + credential)
                    .retrieve()
                    .body(JsonNode.class);
        } catch (RestClientResponseException ex) {
            throw errorMapper.map(ex);
        }

        List<DiscoveredModelDescriptor> discovered = new ArrayList<>();
        if (body == null || !body.has("data") || !body.get("data").isArray()) {
            return discovered;
        }
        for (JsonNode item : body.get("data")) {
            String id = text(item, "id");
            if (id == null || id.isBlank()) {
                continue;
            }
            String displayName = id;
            String family = capabilityMapper.mapFamily(id);
            discovered.add(new DiscoveredModelDescriptor(
                    id,
                    displayName,
                    family,
                    null,
                    capabilityMapper.mapType(id),
                    capabilityMapper.mapContextWindow(id),
                    capabilityMapper.mapMaxOutputTokens(id),
                    capabilityMapper.mapCapabilities(id)));
        }
        return discovered;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull()) {
            return null;
        }
        String text = value.asText();
        return text == null || text.isBlank() ? null : text.trim();
    }
}
